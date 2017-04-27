/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.codec.multipart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.synchronoss.cloud.nio.multipart.Multipart;
import org.synchronoss.cloud.nio.multipart.MultipartContext;
import org.synchronoss.cloud.nio.multipart.MultipartUtils;
import org.synchronoss.cloud.nio.multipart.NioMultipartParser;
import org.synchronoss.cloud.nio.multipart.NioMultipartParserListener;
import org.synchronoss.cloud.nio.stream.storage.StreamStorage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;

/**
 * Implementation of {@link HttpMessageReader} to read multipart HTML
 * forms with {@code "multipart/form-data"} media type in accordance
 * with <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a> based
 * on the Synchronoss NIO Multipart library.
 *
 * @author Sebastien Deleuze
 * @since 5.0
 * @see <a href="https://github.com/synchronoss/nio-multipart">Synchronoss NIO Multipart</a>
 */
public class SynchronossMultipartHttpMessageReader implements MultipartHttpMessageReader {

	@Override
	public Mono<MultiValueMap<String, Part>> readMono(ResolvableType elementType, ReactiveHttpInputMessage inputMessage, Map<String, Object> hints) {

		return Flux.create(new NioMultipartConsumer(inputMessage))
				.collectMultimap(part -> part.getName())
				.map(partsMap -> new LinkedMultiValueMap<>(partsMap
						.entrySet()
						.stream()
						.collect(Collectors.toMap(
							entry -> entry.getKey(),
							entry -> new ArrayList<>(entry.getValue()))
						)));
	}


	private static class NioMultipartConsumer implements Consumer<FluxSink<Part>> {

		private final ReactiveHttpInputMessage inputMessage;


		public NioMultipartConsumer(ReactiveHttpInputMessage inputMessage) {
			this.inputMessage = inputMessage;
		}


		@Override
		public void accept(FluxSink<Part> emitter) {
			HttpHeaders headers = inputMessage.getHeaders();
			MultipartContext context = new MultipartContext(
					headers.getContentType().toString(),
					Math.toIntExact(headers.getContentLength()),
					headers.getFirst(HttpHeaders.ACCEPT_CHARSET));
			NioMultipartParserListener listener = new ReactiveNioMultipartParserListener(emitter);
			NioMultipartParser parser = Multipart.multipart(context).forNIO(listener);

			inputMessage.getBody().subscribe(buffer -> {
				byte[] resultBytes = new byte[buffer.readableByteCount()];
				buffer.read(resultBytes);
				try {
					parser.write(resultBytes);
				}
				catch (IOException ex) {
					listener.onError("Exception thrown while closing the parser", ex);
				}

			}, (e) -> {
				try {
					listener.onError("Exception thrown while reading the request body", e);
					parser.close();
				}
				catch (IOException ex) {
					listener.onError("Exception thrown while closing the parser", ex);
				}
			}, () -> {
				try {
					parser.close();
				}
				catch (IOException ex) {
					listener.onError("Exception thrown while closing the parser", ex);
				}
			});

		}

		private static class ReactiveNioMultipartParserListener implements NioMultipartParserListener {

			private FluxSink<Part> emitter;

			private final AtomicInteger errorCount = new AtomicInteger(0);


			public ReactiveNioMultipartParserListener(FluxSink<Part> emitter) {
				this.emitter = emitter;
			}


			@Override
			public void onPartFinished(StreamStorage streamStorage, Map<String, List<String>> headersFromPart) {
				HttpHeaders headers = new HttpHeaders();
				headers.putAll(headersFromPart);
				emitter.next(new NioPart(headers, streamStorage));
			}

			@Override
			public void onFormFieldPartFinished(String fieldName, String fieldValue, Map<String, List<String>> headersFromPart) {
				HttpHeaders headers = new HttpHeaders();
				headers.putAll(headersFromPart);
				emitter.next(new NioPart(headers, fieldValue));
			}

			@Override
			public void onAllPartsFinished() {
				emitter.complete();
			}

			@Override
			public void onNestedPartStarted(Map<String, List<String>> headersFromParentPart) {
			}

			@Override
			public void onNestedPartFinished() {
			}

			@Override
			public void onError(String message, Throwable cause) {
				if (errorCount.getAndIncrement() == 1) {
					emitter.error(new RuntimeException(message, cause));
				}
			}

		}
	}

	/**
	 * {@link Part} implementation based on the NIO Multipart library.
	 */
	private static class NioPart implements Part {

		private final HttpHeaders headers;

		private final StreamStorage streamStorage;

		private final String content;

		private final DataBufferFactory bufferFactory = new DefaultDataBufferFactory();


		public NioPart(HttpHeaders headers, StreamStorage streamStorage) {
			this.headers = headers;
			this.streamStorage = streamStorage;
			this.content = null;
		}

		public NioPart(HttpHeaders headers, String content) {
			this.headers = headers;
			this.streamStorage = null;
			this.content = content;
		}


		@Override
		public String getName() {
			return MultipartUtils.getFieldName(headers);
		}

		@Override
		public HttpHeaders getHeaders() {
			return this.headers;
		}

		@Override
		public Optional<String> getFilename() {
			return Optional.ofNullable(MultipartUtils.getFileName(this.headers));
		}

		@Override
		public Mono<Void> transferTo(File dest) {
			if (!getFilename().isPresent()) {
				return Mono.error(new IllegalStateException("The part does not contain a file."));
			}
			try {
				InputStream inputStream = this.streamStorage.getInputStream();
				// Get a FileChannel when possible in order to use zero copy mechanism
				ReadableByteChannel inChannel = Channels.newChannel(inputStream);
				FileChannel outChannel = new FileOutputStream(dest).getChannel();
				// NIO Multipart has previously limited the size of the content
				long count = (inChannel instanceof FileChannel ? ((FileChannel)inChannel).size() : Long.MAX_VALUE);
				long result = outChannel.transferFrom(inChannel, 0, count);
				if (result < count) {
					return Mono.error(new IOException(
							"Could only write " + result + " out of " + count + " bytes"));
				}
			}
			catch (IOException ex) {
				return Mono.error(ex);
			}
			return Mono.empty();
		}

		@Override
		public Mono<String> getContentAsString() {
			if (this.content != null) {
				return Mono.just(this.content);
			}
			MediaType contentType = this.headers.getContentType();
			Charset charset = (contentType.getCharset() == null ? StandardCharsets.UTF_8 : contentType.getCharset());
			try {
				return Mono.just(StreamUtils.copyToString(this.streamStorage.getInputStream(), charset));
			}
			catch (IOException e) {
				return Mono.error(new IllegalStateException("Error while reading part content as a string", e));
			}
		}

		@Override
		public Flux<DataBuffer> getContent() {
			if (this.content != null) {
				DataBuffer buffer = this.bufferFactory.allocateBuffer(this.content.length());
				buffer.write(this.content.getBytes());
				return Flux.just(buffer);
			}
			InputStream inputStream = this.streamStorage.getInputStream();
			return DataBufferUtils.read(inputStream, this.bufferFactory, 4096);
		}

	}
}
