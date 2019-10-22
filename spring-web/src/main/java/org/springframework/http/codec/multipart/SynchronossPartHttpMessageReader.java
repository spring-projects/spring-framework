/*
 * Copyright 2002-2018 the original author or authors.
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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
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

	private final DataBufferFactory bufferFactory;

	private final PartBodyStreamStorageFactory streamStorageFactory = new DefaultPartBodyStreamStorageFactory();

	private long maxPartCount = -1;

	private long maxFilePartSize = -1;

	private long maxPartSize = -1;

	public SynchronossPartHttpMessageReader() {
		this.bufferFactory = new DefaultDataBufferFactory();
	}

	SynchronossPartHttpMessageReader(DataBufferFactory bufferFactory) {
		this.bufferFactory = bufferFactory;
	}

	/**
	 * Get the maximum number of parts allowed in a single multipart request.
	 * @since 5.1.11
	 */
	public long getMaxPartCount() {
		return maxPartCount;
	}

	/**
	 * Configure the maximum number of parts allowed in a single multipart request.
	 * @since 5.1.11
	 */
	public void setMaxPartCount(long maxPartCount) {
		this.maxPartCount = maxPartCount;
	}

	/**
	 * Get the maximum size of a file part.
	 * @since 5.1.11
	 */
	public long getMaxFilePartSize() {
		return this.maxFilePartSize;
	}

	/**
	 * Configure the the maximum size of a file part.
	 * @since 5.1.11
	 */
	public void setMaxFilePartSize(long maxFilePartSize) {
		this.maxFilePartSize = maxFilePartSize;
	}

	/**
	 * Get the maximum size of a part.
	 * @since 5.1.11
	 */
	public long getMaxPartSize() {
		return this.maxPartSize;
	}

	/**
	 * Configure the maximum size of a part.
	 * For limits on file parts, use the dedicated {@link #setMaxFilePartSize(long)}.
	 * @since 5.1.11
	 */
	public void setMaxPartSize(long maxPartSize) {
		this.maxPartSize = maxPartSize;
	}

	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return Part.class.equals(elementType.toClass()) &&
				(mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
	}


	@Override
	public Flux<Part> read(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Flux.create(new SynchronossPartGenerator(message, this.bufferFactory, this.streamStorageFactory,
				new MultipartSizeLimiter(getMaxPartCount(), getMaxFilePartSize(), getMaxPartSize())))
				.doOnNext(part -> {
					if (!Hints.isLoggingSuppressed(hints)) {
						LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Parsed " +
								(isEnableLoggingRequestDetails() ?
										LogFormatUtils.formatValue(part, !traceOn) :
										"parts '" + part.name() + "' (content masked)"));
					}
				});
	}


	@Override
	public Mono<Part> readMono(ResolvableType elementType, ReactiveHttpInputMessage message, Map<String, Object> hints) {
		return Mono.error(new UnsupportedOperationException("Cannot read multipart request body into single Part"));
	}


	private static class MultipartSizeLimiter {

		private final long maxPartCount;

		private final long maxFilePartSize;

		private final long maxPartSize;

		private boolean currentIsFilePart;

		private long currentPartCount;

		private long currentPartSize;


		public MultipartSizeLimiter(long maxPartCount, long maxFilePartSize, long maxPartSize) {
			this.maxPartCount = maxPartCount;
			this.maxFilePartSize = maxFilePartSize;
			this.maxPartSize = maxPartSize;
		}

		public void startPart(boolean isFilePart) {
			this.currentPartCount++;
			this.currentIsFilePart = isFilePart;
			if (this.maxPartCount != -1 && this.currentPartCount > this.maxPartCount) {
				throw new IllegalStateException("Exceeded limit on maximum number of multipart parts");
			}
		}

		public void endPart() {
			this.currentPartSize = 0L;
			this.currentIsFilePart = false;
		}

		public void checkCurrentPartSize(long addedBytes) {
			this.currentPartSize += addedBytes;
			if (this.currentIsFilePart && this.maxFilePartSize != -1 && this.currentPartSize > this.maxFilePartSize) {
				throw new IllegalStateException("Exceeded limit on max size of multipart file : " + this.maxFilePartSize);
			}
			else if (!this.currentIsFilePart && this.maxPartSize != -1 && this.currentPartSize > this.maxPartSize) {
				throw new IllegalStateException("Exceeded limit on max size of multipart part : " + this.maxPartSize);
			}
		}

	}

	/**
	 * Consume {@code DataBuffer} as a {@code BaseSubscriber} of the request body
	 * and feed it as input to the Synchronoss parser. Also listen for parser
	 * output events and adapt them to {@code Flux<Sink<Part>>} to emit parts
	 * for subscribers.
	 */
	private static class SynchronossPartGenerator extends BaseSubscriber<DataBuffer>
			implements Consumer<FluxSink<Part>> {

		private final ReactiveHttpInputMessage inputMessage;

		private final DataBufferFactory bufferFactory;

		private final PartBodyStreamStorageFactory streamStorageFactory;

		private final MultipartSizeLimiter limiter;

		private NioMultipartParserListener listener;

		private NioMultipartParser parser;


		public SynchronossPartGenerator(ReactiveHttpInputMessage inputMessage, DataBufferFactory bufferFactory,
				PartBodyStreamStorageFactory streamStorageFactory, MultipartSizeLimiter limiter) {
			this.inputMessage = inputMessage;
			this.bufferFactory = bufferFactory;
			this.streamStorageFactory = new PartBodyStreamStorageFactoryDecorator(streamStorageFactory, limiter);
			this.limiter = limiter;
		}

		@Override
		public void accept(FluxSink<Part> emitter) {
			HttpHeaders headers = this.inputMessage.getHeaders();
			MediaType mediaType = headers.getContentType();
			Assert.state(mediaType != null, "No content type set");

			int length = getContentLength(headers);
			Charset charset = Optional.ofNullable(mediaType.getCharset()).orElse(StandardCharsets.UTF_8);
			MultipartContext context = new MultipartContext(mediaType.toString(), length, charset.name());

			this.listener = new FluxSinkAdapterListener(emitter, this.bufferFactory, context, this.limiter);
			this.parser = Multipart
					.multipart(context)
					.usePartBodyStreamStorageFactory(this.streamStorageFactory)
					// long to int downcast vs. keeping the default 16Kb value
					//.withHeadersSizeLimit(this.limiter.maxPartSize)
					.forNIO(this.listener);
			this.inputMessage.getBody().subscribe(this);
		}

		@Override
		protected void hookOnNext(DataBuffer buffer) {
			int readableByteCount = buffer.readableByteCount();
			this.limiter.checkCurrentPartSize(readableByteCount);
			byte[] resultBytes = new byte[readableByteCount];
			buffer.read(resultBytes);
			try {
				parser.write(resultBytes);
			}
			catch (IOException ex) {
				this.cancel();
				listener.onError("Exception thrown while providing input to the parser", ex);
			}
			finally {
				DataBufferUtils.release(buffer);
			}
		}

		@Override
		protected void hookOnError(Throwable throwable) {
			this.cancel();
			listener.onError("Could not parse multipart request", throwable);
		}

		@Override
		protected void hookOnCancel() {
			this.cancel();
		}

		@Override
		protected void hookFinally(SignalType type) {
			try {
				parser.close();
			}
			catch (IOException ex) {
				listener.onError("Exception thrown while closing the parser", ex);
			}
		}

		private int getContentLength(HttpHeaders headers) {
			// Until this is fixed https://github.com/synchronoss/nio-multipart/issues/10
			long length = headers.getContentLength();
			return (int) length == length ? (int) length : -1;
		}
	}

	private static class PartBodyStreamStorageFactoryDecorator implements PartBodyStreamStorageFactory {

		private final PartBodyStreamStorageFactory streamStorageFactory;

		private final MultipartSizeLimiter limiter;

		public PartBodyStreamStorageFactoryDecorator(PartBodyStreamStorageFactory streamStorageFactory,
				MultipartSizeLimiter limiter) {
			this.streamStorageFactory = streamStorageFactory;
			this.limiter = limiter;
		}

		@Override
		public StreamStorage newStreamStorageForPartBody(Map<String, List<String>> partHeaders, int partIndex) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.putAll(partHeaders);
			String filename = MultipartUtils.getFileName(httpHeaders);
			this.limiter.startPart(filename != null);
			return streamStorageFactory.newStreamStorageForPartBody(partHeaders, partIndex);
		}
	}


	/**
	 * Listen for parser output and adapt to {@code Flux<Sink<Part>>}.
	 */
	private static class FluxSinkAdapterListener implements NioMultipartParserListener {

		private final FluxSink<Part> sink;

		private final DataBufferFactory bufferFactory;

		private final MultipartContext context;

		private final MultipartSizeLimiter limiter;

		private final AtomicInteger terminated = new AtomicInteger(0);

		FluxSinkAdapterListener(FluxSink<Part> sink, DataBufferFactory factory,
				MultipartContext context, MultipartSizeLimiter limiter) {
			this.sink = sink;
			this.bufferFactory = factory;
			this.context = context;
			this.limiter = limiter;
		}

		@Override
		public void onPartFinished(StreamStorage storage, Map<String, List<String>> headers) {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.putAll(headers);
			this.sink.next(createPart(storage, httpHeaders));
			this.limiter.endPart();
		}

		private Part createPart(StreamStorage storage, HttpHeaders httpHeaders) {
			String filename = MultipartUtils.getFileName(httpHeaders);
			if (filename != null) {
				return new SynchronossFilePart(httpHeaders, filename, storage, this.bufferFactory);
			}
			else if (MultipartUtils.isFormField(httpHeaders, this.context)) {
				String value = MultipartUtils.readFormParameterValue(storage, httpHeaders);
				return new SynchronossFormFieldPart(httpHeaders, this.bufferFactory, value);
			}
			else {
				return new SynchronossPart(httpHeaders, storage, this.bufferFactory);
			}
		}

		@Override
		public void onError(String message, Throwable cause) {
			if (this.terminated.getAndIncrement() == 0) {
				this.sink.error(new MultipartException(message, cause));
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

		private final DataBufferFactory bufferFactory;

		AbstractSynchronossPart(HttpHeaders headers, DataBufferFactory bufferFactory) {
			Assert.notNull(headers, "HttpHeaders is required");
			Assert.notNull(bufferFactory, "DataBufferFactory is required");
			this.name = MultipartUtils.getFieldName(headers);
			this.headers = headers;
			this.bufferFactory = bufferFactory;
		}

		@Override
		public String name() {
			return this.name;
		}

		@Override
		public HttpHeaders headers() {
			return this.headers;
		}

		DataBufferFactory getBufferFactory() {
			return this.bufferFactory;
		}

		@Override
		public String toString() {
			return "Part '" + this.name + "', headers=" + this.headers;
		}
	}


	private static class SynchronossPart extends AbstractSynchronossPart {

		private final StreamStorage storage;

		SynchronossPart(HttpHeaders headers, StreamStorage storage, DataBufferFactory factory) {
			super(headers, factory);
			Assert.notNull(storage, "StreamStorage is required");
			this.storage = storage;
		}

		@Override
		public Flux<DataBuffer> content() {
			return DataBufferUtils.readInputStream(getStorage()::getInputStream, getBufferFactory(), 4096);
		}

		protected StreamStorage getStorage() {
			return this.storage;
		}
	}


	private static class SynchronossFilePart extends SynchronossPart implements FilePart {

		private static final OpenOption[] FILE_CHANNEL_OPTIONS =
				{StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE};

		private final String filename;

		SynchronossFilePart(HttpHeaders headers, String filename, StreamStorage storage, DataBufferFactory factory) {
			super(headers, storage, factory);
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

		SynchronossFormFieldPart(HttpHeaders headers, DataBufferFactory bufferFactory, String content) {
			super(headers, bufferFactory);
			this.content = content;
		}

		@Override
		public String value() {
			return this.content;
		}

		@Override
		public Flux<DataBuffer> content() {
			byte[] bytes = this.content.getBytes(getCharset());
			DataBuffer buffer = getBufferFactory().allocateBuffer(bytes.length);
			buffer.write(bytes);
			return Flux.just(buffer);
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
