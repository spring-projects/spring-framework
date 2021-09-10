/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.synchronoss.cloud.nio.multipart.DefaultPartBodyStreamStorageFactory;
import org.synchronoss.cloud.nio.multipart.Multipart;
import org.synchronoss.cloud.nio.multipart.MultipartContext;
import org.synchronoss.cloud.nio.multipart.MultipartUtils;
import org.synchronoss.cloud.nio.multipart.NioMultipartParser;
import org.synchronoss.cloud.nio.multipart.NioMultipartParserListener;
import org.synchronoss.cloud.nio.multipart.PartBodyStreamStorageFactory;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;
import reactor.core.scheduler.Schedulers;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@code HttpMessageReader} for parsing {@code "multipart/form-data"} requests
 * to a stream of {@link Part}'s using the Synchronoss NIO Multipart library.
 *
 * <p>This reader can be provided to {@link MultipartHttpMessageReader} in order
 * to aggregate all parts into a Map.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 * @see <a href="https://github.com/synchronoss/nio-multipart">Synchronoss NIO Multipart</a>
 * @see MultipartHttpMessageReader
 */
public class SynchronossPartHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<Part> {

	private static final String FILE_STORAGE_DIRECTORY_PREFIX = "synchronoss-file-upload-";

	private int maxInMemorySize = 256 * 1024;

	private long maxDiskUsagePerPart = -1;

	private int maxParts = -1;

	private final AtomicReference<Path> fileStorageDirectory = new AtomicReference<>();


	/**
	 * Configure the maximum amount of memory that is allowed to use per part.
	 * When the limit is exceeded:
	 * <ul>
	 * <li>file parts are written to a temporary file.
	 * <li>non-file parts are rejected with {@link DataBufferLimitException}.
	 * </ul>
	 * <p>By default this is set to 256K.
	 * @param byteCount the in-memory limit in bytes; if set to -1 this limit is
	 * not enforced, and all parts may be written to disk and are limited only
	 * by the {@link #setMaxDiskUsagePerPart(long) maxDiskUsagePerPart} property.
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * Get the {@link #setMaxInMemorySize configured} maximum in-memory size.
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	/**
	 * Configure the maximum amount of disk space allowed for file parts.
	 * <p>By default this is set to -1.
	 * @param maxDiskUsagePerPart the disk limit in bytes, or -1 for unlimited
	 * @since 5.1.11
	 */
	public void setMaxDiskUsagePerPart(long maxDiskUsagePerPart) {
		this.maxDiskUsagePerPart = maxDiskUsagePerPart;
	}

	/**
	 * Get the {@link #setMaxDiskUsagePerPart configured} maximum disk usage.
	 * @since 5.1.11
	 */
	public long getMaxDiskUsagePerPart() {
		return this.maxDiskUsagePerPart;
	}

	/**
	 * Specify the maximum number of parts allowed in a given multipart request.
	 * @since 5.1.11
	 */
	public void setMaxParts(int maxParts) {
		this.maxParts = maxParts;
	}

	/**
	 * Return the {@link #setMaxParts configured} limit on the number of parts.
	 * @since 5.1.11
	 */
	public int getMaxParts() {
		return this.maxParts;
	}

	/**
	 * Set the directory used to store parts larger than
	 * {@link #setMaxInMemorySize(int) maxInMemorySize}. By default, a new
	 * temporary directory is created.
	 * @throws IOException if an I/O error occurs, or the parent directory
	 * does not exist
	 * @since 5.3.7
	 */
	public void setFileStorageDirectory(Path fileStorageDirectory) throws IOException {
		Assert.notNull(fileStorageDirectory, "FileStorageDirectory must not be null");
		if (!Files.exists(fileStorageDirectory)) {
			Files.createDirectory(fileStorageDirectory);
		}
		this.fileStorageDirectory.set(fileStorageDirectory);
	}


	@Override
	public List<MediaType> getReadableMediaTypes() {
		return MultipartHttpMessageReader.MIME_TYPES;
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		if (Part.class.equals(elementType.toClass())) {
			if (mediaType == null) {
				return true;
			}
			for (MediaType supportedMediaType : getReadableMediaTypes()) {
				if (supportedMediaType.isCompatibleWith(mediaType)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return getFileStorageDirectory().flatMapMany(directory ->
				Flux.create(new SynchronossPartGenerator(message, directory))
						.doOnNext(part -> {
							if (!Hints.isLoggingSuppressed(hints)) {
								LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Parsed " +
										(isEnableLoggingRequestDetails() ?
												LogFormatUtils.formatValue(part, !traceOn) :
												"parts '" + part.name() + "' (content masked)"));
							}
						}));
	}

	@Override
	public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
	}

	private Mono<Path> getFileStorageDirectory() {
		return Mono.defer(() -> {
			Path directory = this.fileStorageDirectory.get();
			if (directory != null) {
				return Mono.just(directory);
			}
			else {
				return Mono.fromCallable(() -> {
					Path tempDirectory = Files.createTempDirectory(FILE_STORAGE_DIRECTORY_PREFIX);
					if (this.fileStorageDirectory.compareAndSet(null, tempDirectory)) {
						return tempDirectory;
					}
					else {
						try {
							Files.delete(tempDirectory);
						}
						catch (IOException ignored) {
						}
						return this.fileStorageDirectory.get();
					}
				}).subscribeOn(Schedulers.boundedElastic());
			}
		});
	}


	/**
	 * Subscribe to the input stream and feed the Synchronoss parser. Then listen
	 * for parser output, creating parts, and pushing them into the FluxSink.
	 */
	private class SynchronossPartGenerator extends BaseSubscriber<DataBuffer> implements Consumer<FluxSink<Part>> {

		private final ReactiveHttpInputMessage inputMessage;

		private final LimitedPartBodyStreamStorageFactory storageFactory = new LimitedPartBodyStreamStorageFactory();

		private final Path fileStorageDirectory;

		@Nullable
		private NioMultipartParserListener listener;

		@Nullable
		private NioMultipartParser parser;

		public SynchronossPartGenerator(ReactiveHttpInputMessage inputMessage, Path fileStorageDirectory) {
			this.inputMessage = inputMessage;
			this.fileStorageDirectory = fileStorageDirectory;
		}

		@Override
		public void accept(FluxSink<Part> sink) {
			HttpHeaders headers = this.inputMessage.getHeaders();
			MediaType mediaType = headers.getContentType();
			Assert.state(mediaType != null, "No content type set");

			int length = getContentLength(headers);
			Charset charset = Optional.ofNullable(mediaType.getCharset()).orElse(StandardCharsets.UTF_8);
			MultipartContext context = new MultipartContext(mediaType.toString(), length, charset.name());

			this.listener = new FluxSinkAdapterListener(sink, context, this.storageFactory);

			this.parser = Multipart
					.multipart(context)
					.saveTemporaryFilesTo(this.fileStorageDirectory.toString())
					.usePartBodyStreamStorageFactory(this.storageFactory)
					.forNIO(this.listener);

			this.inputMessage.getBody().subscribe(this);
		}

		@Override
		protected void hookOnNext(DataBuffer buffer) {
			Assert.state(this.parser != null && this.listener != null, "Not initialized yet");

			int size = buffer.readableByteCount();
			this.storageFactory.increaseByteCount(size);
			byte[] resultBytes = new byte[size];
			buffer.read(resultBytes);

			try {
				this.parser.write(resultBytes);
			}
			catch (IOException ex) {
				cancel();
				int index = this.storageFactory.getCurrentPartIndex();
				this.listener.onError("Parser error for part [" + index + "]", ex);
			}
			finally {
				DataBufferUtils.release(buffer);
			}
		}

		@Override
		protected void hookOnError(Throwable ex) {
			if (this.listener != null) {
				int index = this.storageFactory.getCurrentPartIndex();
				this.listener.onError("Failure while parsing part[" + index + "]", ex);
			}
		}

		@Override
		protected void hookOnComplete() {
			if (this.listener != null) {
				this.listener.onAllPartsFinished();
			}
		}

		@Override
		protected void hookFinally(SignalType type) {
			try {
				if (this.parser != null) {
					this.parser.close();
				}
			}
			catch (IOException ex) {
				// ignore
			}
		}

		private int getContentLength(HttpHeaders headers) {
			// Until this is fixed https://github.com/synchronoss/nio-multipart/issues/10
			long length = headers.getContentLength();
			return (int) length == length ? (int) length : -1;
		}
	}


	private class LimitedPartBodyStreamStorageFactory implements PartBodyStreamStorageFactory {

		private final PartBodyStreamStorageFactory storageFactory = (maxInMemorySize > 0 ?
				new DefaultPartBodyStreamStorageFactory(maxInMemorySize) :
				new DefaultPartBodyStreamStorageFactory());

		private int index = 1;

		private boolean isFilePart;

		private long partSize;

		public int getCurrentPartIndex() {
			return this.index;
		}

		@Override
		public StreamStorage newStreamStorageForPartBody(Map<String, List<String>> headers, int index) {
			this.index = index;
			this.isFilePart = (MultipartUtils.getFileName(headers) != null);
			this.partSize = 0;
			if (maxParts > 0 && index > maxParts) {
				throw new DecodingException("Too many parts: Part[" + index + "] but maxParts=" + maxParts);
			}
			return this.storageFactory.newStreamStorageForPartBody(headers, index);
		}

		public void increaseByteCount(long byteCount) {
			this.partSize += byteCount;
			if (maxInMemorySize > 0 && !this.isFilePart && this.partSize >= maxInMemorySize) {
				throw new DataBufferLimitException("Part[" + this.index + "] " +
						"exceeded the in-memory limit of " + maxInMemorySize + " bytes");
			}
			if (maxDiskUsagePerPart > 0 && this.isFilePart && this.partSize > maxDiskUsagePerPart) {
				throw new DecodingException("Part[" + this.index + "] " +
						"exceeded the disk usage limit of " + maxDiskUsagePerPart + " bytes");
			}
		}

		public void partFinished() {
			this.index++;
			this.isFilePart = false;
			this.partSize = 0;
		}
	}


	/**
	 * Listen for parser output and adapt to {@code Flux<Sink<Part>>}.
	 */
	private static class FluxSinkAdapterListener implements NioMultipartParserListener {

		private final FluxSink<Part> sink;

		private final MultipartContext context;

		private final LimitedPartBodyStreamStorageFactory storageFactory;

		private final AtomicInteger terminated = new AtomicInteger();

		FluxSinkAdapterListener(
				FluxSink<Part> sink, MultipartContext context, LimitedPartBodyStreamStorageFactory factory) {

			this.sink = sink;
			this.context = context;
			this.storageFactory = factory;
		}

		@Override
		public void onPartFinished(StreamStorage storage, Map<String, List<String>> headers) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.putAll(headers);
			this.storageFactory.partFinished();
			this.sink.next(createPart(storage, httpHeaders));
		}

		private Part createPart(StreamStorage storage, HttpHeaders httpHeaders) {
			String filename = MultipartUtils.getFileName(httpHeaders);
			if (filename != null) {
				return new SynchronossFilePart(httpHeaders, filename, storage);
			}
			else if (MultipartUtils.isFormField(httpHeaders, this.context)) {
				String value = MultipartUtils.readFormParameterValue(storage, httpHeaders);
				return new SynchronossFormFieldPart(httpHeaders, value);
			}
			else {
				return new SynchronossPart(httpHeaders, storage);
			}
		}

		@Override
		public void onError(String message, Throwable cause) {
			if (this.terminated.getAndIncrement() == 0) {
				this.sink.error(new DecodingException(message, cause));
			}
		}

		@Override
		public void onAllPartsFinished() {
			if (this.terminated.getAndIncrement() == 0) {
				this.sink.complete();
			}
		}

		@Override
		public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
		}

		@Override
		public void onNestedPartFinished() {
		}
	}


	private abstract static class AbstractSynchronossPart implements Part {

		private final String name;

		private final HttpHeaders headers;

		AbstractSynchronossPart(HttpHeaders headers) {
			Assert.notNull(headers, "HttpHeaders is required");
			this.name = MultipartUtils.getFieldName(headers);
			this.headers = headers;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		@Override
		public String toString() {
			return "Part '" + this.name + "', headers=" + this.headers;
		}
	}


	private static class SynchronossPart extends AbstractSynchronossPart {

		private final StreamStorage storage;

		SynchronossPart(HttpHeaders headers, StreamStorage storage) {
			super(headers);
			Assert.notNull(storage, "StreamStorage is required");
			this.storage = storage;
		}

		@Override
		@SuppressWarnings("resource")
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readInputStream(
					getStorage()::getInputStream, DefaultDataBufferFactory.sharedInstance, 4096);
		}

		protected StreamStorage getStorage() {
			return this.storage;
		}
	}


	private static class SynchronossFilePart extends SynchronossPart implements FilePart {

		private static final OpenOption[] FILE_CHANNEL_OPTIONS =
				{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

		private final String filename;

		SynchronossFilePart(HttpHeaders headers, String filename, StreamStorage storage) {
			super(headers, storage);
			this.filename = filename;
		}

		@Override
		public String filename() {
			return this.filename;
		}

		@Override
		public Mono<Void> transferTo(Path dest) {
			ReadableByteChannel input = null;
			FileChannel output = null;
			try {
				input = Channels.newChannel(getStorage().getInputStream());
				output = FileChannel.open(dest, FILE_CHANNEL_OPTIONS);
				long size = (input instanceof FileChannel ? ((FileChannel) input).size() : Long.MAX_VALUE);
				long totalWritten = 0;
				while (totalWritten < size) {
					long written = output.transferFrom(input, totalWritten, size - totalWritten);
					if (written <= 0) {
						break;
					}
					totalWritten += written;
				}
			}
			catch (IOException ex) {
				return Mono.error(ex);
			}
			finally {
				if (input != null) {
					try {
						input.close();
					}
					catch (IOException ignored) {
					}
				}
				if (output != null) {
					try {
						output.close();
					}
					catch (IOException ignored) {
					}
				}
			}
			return Mono.empty();
		}

		@Override
		public String toString() {
			return "Part '" + name() + "', filename='" + this.filename + "'";
		}
	}


	private static class SynchronossFormFieldPart extends AbstractSynchronossPart implements FormFieldPart {

		private final String content;

		SynchronossFormFieldPart(HttpHeaders headers, String content) {
			super(headers);
			this.content = content;
		}

		@Override
		public String value() {
			return this.content;
		}

		@Override
		public Flux<DataBuffer> content() {
			byte[] bytes = this.content.getBytes(getCharset());
			return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
		}

		private Charset getCharset() {
			String name = MultipartUtils.getCharEncoding(headers());
			return (name != null ? Charset.forName(name) : StandardCharsets.UTF_8);
		}

		@Override
		public String toString() {
			return "Part '" + name() + "=" + this.content + "'";
		}
	}

}
