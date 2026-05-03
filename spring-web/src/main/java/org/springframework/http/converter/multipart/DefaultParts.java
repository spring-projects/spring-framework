/*
 * Copyright 2002-present the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

/**
 * Default implementations of {@link Part} and subtypes.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
abstract class DefaultParts {

	/**
	 * Create a new {@link FormFieldPart} with the given parameters.
	 * @param headers the part headers
	 * @param value the form field value
	 * @return the created part
	 */
	public static FormFieldPart formFieldPart(HttpHeaders headers, String value) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(value, "Value must not be null");

		return new DefaultFormFieldPart(headers, value);
	}

	/**
	 * Create a new {@link Part} or {@link FilePart} based on a flux of data
	 * buffers. Returns {@link FilePart} if the {@code Content-Disposition} of
	 * the given headers contains a filename, or a "normal" {@link Part}
	 * otherwise.
	 * @param headers the part headers
	 * @param dataBuffer the content of the part
	 * @return {@link Part} or {@link FilePart}, depending on {@link HttpHeaders#getContentDisposition()}
	 */
	public static Part part(HttpHeaders headers, DataBuffer dataBuffer) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(dataBuffer, "DataBuffer must not be null");

		return partInternal(headers, new DataBufferContent(dataBuffer));
	}

	/**
	 * Create a new {@link Part} or {@link FilePart} based on the given file.
	 * Returns {@link FilePart} if the {@code Content-Disposition} of the given
	 * headers contains a filename, or a "normal" {@link Part} otherwise
	 * @param headers the part headers
	 * @param file  the file
	 * @return {@link Part} or {@link FilePart}, depending on {@link HttpHeaders#getContentDisposition()}
	 */
	public static Part part(HttpHeaders headers, Path file) {
		Assert.notNull(headers, "Headers must not be null");
		Assert.notNull(file, "File must not be null");

		return partInternal(headers, new FileContent(file));
	}


	private static Part partInternal(HttpHeaders headers, Content content) {
		String filename = headers.getContentDisposition().getFilename();
		if (filename != null) {
			return new DefaultFilePart(headers, content);
		}
		else {
			return new DefaultPart(headers, content);
		}
	}


	/**
	 * Abstract base class for {@link Part} implementations.
	 */
	private abstract static class AbstractPart implements Part {

		private final HttpHeaders headers;

		protected AbstractPart(HttpHeaders headers) {
			Assert.notNull(headers, "HttpHeaders is required");
			this.headers = headers;
		}

		@Override
		public String name() {
			String name = headers().getContentDisposition().getName();
			Assert.state(name != null, "No part name available");
			return name;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}
	}


	/**
	 * Default implementation of {@link FormFieldPart}.
	 */
	private static class DefaultFormFieldPart extends AbstractPart implements FormFieldPart {

		private final String value;

		public DefaultFormFieldPart(HttpHeaders headers, String value) {
			super(headers);
			this.value = value;
		}

		@Override
		public InputStream content() {
			byte[] bytes = this.value.getBytes(MultipartUtils.charset(headers()));
			return new ByteArrayInputStream(bytes);
		}

		@Override
		public String value() {
			return this.value;
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultFormFieldPart{" + name() + "}";
			}
			else {
				return "DefaultFormFieldPart";
			}
		}
	}


	/**
	 * Default implementation of {@link Part}.
	 */
	private static class DefaultPart extends AbstractPart {

		protected final Content content;

		public DefaultPart(HttpHeaders headers, Content content) {
			super(headers);
			this.content = content;
		}

		@Override
		public InputStream content() throws IOException {
			return this.content.content();
		}

		@Override
		public void delete() throws IOException {
			this.content.delete();
		}

		@Override
		public String toString() {
			String name = headers().getContentDisposition().getName();
			if (name != null) {
				return "DefaultPart{" + name + "}";
			}
			else {
				return "DefaultPart";
			}
		}
	}


	/**
	 * Default implementation of {@link FilePart}.
	 */
	private static final class DefaultFilePart extends DefaultPart implements FilePart {

		public DefaultFilePart(HttpHeaders headers, Content content) {
			super(headers, content);
		}

		@Override
		public String filename() {
			String filename = headers().getContentDisposition().getFilename();
			Assert.state(filename != null, "No filename found");
			return filename;
		}

		@Override
		public void transferTo(Path dest) throws IOException {
			this.content.transferTo(dest);
		}

		@Override
		public String toString() {
			ContentDisposition contentDisposition = headers().getContentDisposition();
			String name = contentDisposition.getName();
			String filename = contentDisposition.getFilename();
			if (name != null) {
				return "DefaultFilePart{" + name + " (" + filename + ")}";
			}
			else {
				return "DefaultFilePart{(" + filename + ")}";
			}
		}
	}


	/**
	 * Part content abstraction.
	 */
	private interface Content {

		InputStream content() throws IOException;

		void transferTo(Path dest) throws IOException;

		void delete() throws IOException;
	}


	/**
	 * {@code Content} implementation based on an in-memory {@code InputStream}.
	 */
	private static final class DataBufferContent implements Content {

		private final DataBuffer content;

		public DataBufferContent(DataBuffer content) {
			this.content = content;
		}

		@Override
		public InputStream content() {
			return this.content.asInputStream();
		}

		@Override
		public void transferTo(Path dest) throws IOException {
			Files.copy(this.content.asInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
		}

		@Override
		public void delete() throws IOException {
		}
	}


	/**
	 * {@code Content} implementation based on a file.
	 */
	private static final class FileContent implements Content {

		private final Path file;

		public FileContent(Path file) {
			this.file = file;
		}

		@Override
		public InputStream content() throws IOException {
			return Files.newInputStream(this.file.toAbsolutePath(), StandardOpenOption.READ);
		}

		@Override
		public void transferTo(Path dest) throws IOException {
			Files.copy(this.file, dest, StandardCopyOption.REPLACE_EXISTING);
		}

		@Override
		public void delete() throws IOException {
			Files.delete(this.file);
		}

	}

}
