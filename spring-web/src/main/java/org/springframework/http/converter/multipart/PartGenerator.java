/*
 * Copyright 2026-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link MultipartParser.PartListener Listen} to a stream of part tokens
 * and return a {@code MultiValueMap<String, Part>} as a result.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 */
final class PartGenerator implements MultipartParser.PartListener {

	private static final Log logger = LogFactory.getLog(PartGenerator.class);

	private final MultiValueMap<String, Part> parts = new LinkedMultiValueMap<>();

	private final int maxInMemorySize;

	private final long maxDiskUsagePerPart;

	private final int maxParts;

	private final Path fileStorageDirectory;

	private int partCount;

	private State state;


	PartGenerator(int maxInMemorySize, long maxDiskUsagePerPart, int maxParts, Path fileStorageDirectory) {
		this.maxInMemorySize = maxInMemorySize;
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
		this.maxParts = maxParts;
		this.fileStorageDirectory = fileStorageDirectory;
		this.state = new InitialState();
	}

	/**
	 * Return the collected parts.
	 */
	public MultiValueMap<String, Part> getParts() {
		return this.parts;
	}

	@Override
	public void onHeaders(HttpHeaders headers) {
		if (isFormField(headers)) {
			this.state = new FormFieldState(headers);
		}
		else {
			this.state = new InMemoryState(headers);
		}
	}

	private static boolean isFormField(HttpHeaders headers) {
		MediaType contentType = headers.getContentType();
		return (contentType == null || MediaType.TEXT_PLAIN.equalsTypeAndSubtype(contentType)) &&
				headers.getContentDisposition().getFilename() == null;
	}

	@Override
	public void onBody(DataBuffer buffer, boolean last) {
		try {
			this.state.onBody(buffer, last);
		}
		catch (Throwable ex) {
			deleteParts();
			throw ex;
		}
	}

	void deleteParts() {
		try {
			for (List<Part> partList : this.parts.values()) {
				for (Part part : partList) {
					part.delete();
				}
			}
		}
		catch (IOException ex) {
			// ignored
		}
	}

	@Override
	public void onComplete() {
		if (logger.isTraceEnabled()) {
			logger.trace("Finished reading " + this.partCount + " part(s)");
		}
	}

	@Override
	public void onError(Throwable error) {
		deleteParts();
		throw new HttpMessageConversionException("Cannot decode multipart body", error);
	}

	void addPart(Part part) {
		if (this.maxParts != -1 && this.partCount == this.maxParts) {
			throw new HttpMessageConversionException("Maximum number of parts exceeded: " + this.maxParts);
		}
		try {
			this.partCount++;
			this.parts.add(part.name(), part);
		}
		catch (Exception exc) {
			throw new HttpMessageConversionException("Part #" + this.partCount + " is unnamed", exc);
		}
	}

	/**
	 * Represents the internal state of the {@link PartGenerator} for creating a single {@link Part}.
	 * {@link State} instances are stateful, and created when a new
	 * {@link MultipartParser.PartListener#onHeaders(HttpHeaders) headers instance} is accepted.
	 * The following rules determine which state the creator will have:
	 * <ol>
	 * <li>If the part is a {@linkplain #isFormField(HttpHeaders) form field},
	 * the creator will be in the {@link FormFieldState}.</li>
	 * <li>Otherwise, the creator will initially be in the
	 * {@link InMemoryState}, but will switch over to {@link FileState}
	 * when the part byte count exceeds {@link #maxInMemorySize}</li>
	 * </ol>
	 */
	private interface State {

		/**
		 * Invoked when a {@link MultipartParser.PartListener#onBody(DataBuffer, boolean)} is received.
		 */
		void onBody(DataBuffer dataBuffer, boolean last);

	}

	/**
	 * The initial state of the creator. Throws an exception for {@link #onBody(DataBuffer, boolean)}.
	 */
	private static final class InitialState implements State {

		private InitialState() {
		}

		@Override
		public void onBody(DataBuffer dataBuffer, boolean last) {
			DataBufferUtils.release(dataBuffer);
			throw new HttpMessageConversionException("Body token not expected");
		}

		@Override
		public String toString() {
			return "INITIAL";
		}
	}

	/**
	 * The creator state when a form field is received.
	 * Stores all body buffers in memory (up until {@link #maxInMemorySize}).
	 */
	private final class FormFieldState implements State {

		private final FastByteArrayOutputStream value = new FastByteArrayOutputStream();

		private final HttpHeaders headers;

		public FormFieldState(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void onBody(DataBuffer dataBuffer, boolean last) {
			int size = this.value.size() + dataBuffer.readableByteCount();
			if (PartGenerator.this.maxInMemorySize == -1 ||
					size < PartGenerator.this.maxInMemorySize) {
				store(dataBuffer);
			}
			else {
				DataBufferUtils.release(dataBuffer);
				throw new HttpMessageConversionException("Form field value exceeded the memory usage limit of " +
						PartGenerator.this.maxInMemorySize + " bytes");
			}
			if (last) {
				byte[] bytes = this.value.toByteArrayUnsafe();
				String value = new String(bytes, MultipartUtils.charset(this.headers));
				FormFieldPart formFieldPart = DefaultParts.formFieldPart(this.headers, value);
				PartGenerator.this.addPart(formFieldPart);
			}
		}

		private void store(DataBuffer dataBuffer) {
			try {
				byte[] bytes = new byte[dataBuffer.readableByteCount()];
				dataBuffer.read(bytes);
				this.value.write(bytes);
			}
			catch (IOException ex) {
				throw new HttpMessageConversionException("Cannot store multipart body", ex);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		@Override
		public String toString() {
			return "FORM-FIELD";
		}
	}

	/**
	 * The creator state when not handling a form field.
	 * Stores all received buffers in a queue.
	 * If the byte count exceeds {@link #maxInMemorySize}, the creator state
	 * is changed to {@link FileState}.
	 */
	private final class InMemoryState implements State {

		private final Queue<DataBuffer> content = new ArrayDeque<>();

		private long byteCount;

		private final HttpHeaders headers;


		public InMemoryState(HttpHeaders headers) {
			this.headers = headers;
		}

		@Override
		public void onBody(DataBuffer dataBuffer, boolean last) {
			this.byteCount += dataBuffer.readableByteCount();
			if (PartGenerator.this.maxInMemorySize == -1 ||
					this.byteCount <= PartGenerator.this.maxInMemorySize) {
				this.content.add(dataBuffer);
				if (last) {
					emitMemoryPart();
				}
			}
			else {
				switchToFile(dataBuffer, last);
			}
		}

		private void switchToFile(DataBuffer current, boolean last) {
			FileState newState = new FileState(this.headers, PartGenerator.this.fileStorageDirectory);
			this.content.forEach(newState::writeBuffer);
			newState.onBody(current, last);
			PartGenerator.this.state = newState;
		}

		private void emitMemoryPart() {
			byte[] bytes = new byte[(int) this.byteCount];
			int idx = 0;
			for (DataBuffer buffer : this.content) {
				int len = buffer.readableByteCount();
				buffer.read(bytes, idx, len);
				idx += len;
				DataBufferUtils.release(buffer);
			}
			this.content.clear();
			DefaultDataBuffer content = DefaultDataBufferFactory.sharedInstance.wrap(bytes);
			Part part = DefaultParts.part(this.headers, content);
			PartGenerator.this.addPart(part);
		}

		@Override
		public String toString() {
			return "IN-MEMORY";
		}
	}

	/**
	 * The creator state when writing for a temporary file.
	 * {@link InMemoryState} initially switches to this state when the byte
	 * count exceeds {@link #maxInMemorySize}.
	 */
	private final class FileState implements State {

		private final HttpHeaders headers;

		private final Path file;

		private final OutputStream outputStream;

		private long byteCount;


		public FileState(HttpHeaders headers, Path folder) {
			this.headers = headers;
			this.file = createFile(folder);
			this.outputStream = createOutputStream(this.file);
		}

		@Override
		public void onBody(DataBuffer dataBuffer, boolean last) {
			this.byteCount += dataBuffer.readableByteCount();
			if (PartGenerator.this.maxDiskUsagePerPart == -1 || this.byteCount <= PartGenerator.this.maxDiskUsagePerPart) {
				writeBuffer(dataBuffer);
				if (last) {
					Part part = DefaultParts.part(this.headers, this.file);
					PartGenerator.this.addPart(part);
				}
			}
			else {
				try {
					this.outputStream.close();
				}
				catch (IOException exc) {
					// ignored
				}
				throw new HttpMessageConversionException("Part exceeded the disk usage limit of " +
						PartGenerator.this.maxDiskUsagePerPart + " bytes");
			}
		}

		private Path createFile(Path directory) {
			try {
				Path tempFile = Files.createTempFile(directory, null, ".multipart");
				if (logger.isTraceEnabled()) {
					logger.trace("Storing multipart data in file " + tempFile);
				}
				return tempFile;
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Could not create temp file in " + directory, ex);
			}
		}

		private OutputStream createOutputStream(Path file) {
			try {
				return Files.newOutputStream(file, StandardOpenOption.WRITE);
			}
			catch (IOException ex) {
				throw new UncheckedIOException("Could not write to temp file " + file, ex);
			}
		}

		private void writeBuffer(DataBuffer dataBuffer) {
			try (InputStream in = dataBuffer.asInputStream()) {
				in.transferTo(this.outputStream);
				this.outputStream.flush();
			}
			catch (IOException exc) {
				throw new UncheckedIOException("Could not write to temp file ", exc);
			}
			finally {
				DataBufferUtils.release(dataBuffer);
			}
		}

		@Override
		public String toString() {
			return "WRITE-FILE";
		}
	}

}
