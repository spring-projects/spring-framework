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

package org.springframework.http.codec.multipart;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.util.Assert;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.util.MimeTypeUtils;

/**
 * Support class for multipart HTTP message writers.
 *
 * @author Rossen Stoyanchev
 * @since 5.3
 */
public class MultipartWriterSupport extends LoggingCodecSupport {

	/** THe default charset used by the writer. */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private final List<MediaType> supportedMediaTypes;

	private Charset charset = DEFAULT_CHARSET;


	/**
	 * Constructor with the list of supported media types.
	 */
	protected MultipartWriterSupport(List<MediaType> supportedMediaTypes) {
		this.supportedMediaTypes = supportedMediaTypes;
	}


	/**
	 * Return the configured charset for part headers.
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * Set the character set to use for part headers such as
	 * "Content-Disposition" (and its filename parameter).
	 * <p>By default this is set to "UTF-8". If changed from this default,
	 * the "Content-Type" header will have a "charset" parameter that specifies
	 * the character set used.
	 */
	public void setCharset(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.charset = charset;
	}

	/**
	 * Return the media types supported by this writer.
	 * @return the supported media types
	 * @since 5.3
	 */
	public List<MediaType> getWritableMediaTypes() {
		return this.supportedMediaTypes;
	}

	/**
	 * Generate a multipart boundary.
	 * <p>By default delegates to {@link MimeTypeUtils#generateMultipartBoundary()}.
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

	/**
	 * Prepare the {@code MediaType} to use by adding "boundary" and "charset"
	 * parameters to the given {@code mediaType} or "multipart/form-data"
	 * otherwise by default.
	 */
	protected MediaType getMultipartMediaType(@Nullable MediaType mediaType, byte[] boundary) {
		Map<String, String> params = new HashMap<>();
		if (mediaType != null) {
			params.putAll(mediaType.getParameters());
		}
		params.put("boundary", new String(boundary, StandardCharsets.US_ASCII));
		Charset charset = getCharset();
		if (!charset.equals(StandardCharsets.UTF_8) &&
				!charset.equals(StandardCharsets.US_ASCII) ) {
			params.put("charset", charset.name());
		}

		mediaType = (mediaType != null ? mediaType : MediaType.MULTIPART_FORM_DATA);
		mediaType = new MediaType(mediaType, params);
		return mediaType;
	}

	/**
	 * Write encoded part headers and bodies as a multipart message, flushing
	 * after each part.
	 * <p>Each part publisher must emit its encoded headers, an empty line, and
	 * its body, without the surrounding multipart framing. Headers and body
	 * may be emitted as multiple buffers. This method adds the framing CRLFs
	 * and boundaries, and completes the closing boundary when the parts finish
	 * successfully. An empty parts stream produces only a closing boundary.
	 * <p>The caller must set the multipart content type on the output message,
	 * including a boundary parameter that matches the supplied boundary.
	 * @param parts the encoded part publishers in the order to write them
	 * @param boundary the multipart boundary
	 * @param outputMessage the message to write to
	 * @param hints additional information about how to write
	 * @return a {@code Mono} that signals completion or failure of the write
	 * @since 7.1
	 */
	protected Mono<Void> writeMultipart(
			Publisher<? extends Publisher<? extends DataBuffer>> parts, byte[] boundary,
			ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

		DataBufferFactory bufferFactory = outputMessage.bufferFactory();
		Flux<Publisher<DataBuffer>> body = Flux.from(parts)
				.<Publisher<DataBuffer>>map(part -> Flux.concat(
						generateNewLine(bufferFactory),
						part,
						generateNewLine(bufferFactory),
						generateBoundary(boundary, bufferFactory)))
				.switchOnFirst((signal, flux) -> {
					if (signal.hasValue()) {
						return flux.startWith(Mono.just(generateBoundary(boundary, bufferFactory)))
								.concatWith(Mono.just(generateLastLineSuffix(bufferFactory)));
					}
					else {
						return flux.concatWith(Mono.just(generateLastLine(boundary, bufferFactory)));
					}
				});

		return outputMessage.writeAndFlushWith(body.map(part -> {
			Flux<DataBuffer> buffers = Flux.from(part)
					.doOnDiscard(DataBuffer.class, DataBufferUtils::release);
			if (logger.isDebugEnabled()) {
				buffers = buffers.doOnNext(buffer -> Hints.touchDataBuffer(buffer, hints, logger));
			}
			return buffers;
		}));
	}

	/**
	 * Generate two hyphens followed by the boundary, without a trailing CRLF.
	 * @param boundary the multipart boundary
	 * @param bufferFactory the buffer factory to use
	 * @return a publisher for the boundary buffer
	 * @since 7.1
	 */
	protected Mono<DataBuffer> generateBoundary(byte[] boundary, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 2);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write(boundary);
			return buffer;
		});
	}

	/**
	 * Generate two hyphens followed by CRLF to complete a closing boundary.
	 * <p>The boundary prefix from {@link #generateBoundary} must already
	 * have been written.
	 * @param bufferFactory the buffer factory to use
	 * @return a publisher for the closing boundary suffix buffer
	 * @since 7.1
	 */
	protected Mono<DataBuffer> generateLastLineSuffix(DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(4);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

	/**
	 * Generate two hyphens followed by the boundary and a trailing CRLF.
	 * @param boundary the multipart boundary
	 * @param bufferFactory the buffer factory to use
	 * @return a publisher for the boundary line buffer
	 * @since 5.3
	 */
	protected Mono<DataBuffer> generateBoundaryLine(byte[] boundary, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 4);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write(boundary);
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

	/**
	 * Generate a CRLF line ending.
	 * @param bufferFactory the buffer factory to use
	 * @return a publisher for the line ending buffer
	 * @since 5.3
	 */
	protected Mono<DataBuffer> generateNewLine(DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(2);
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

	/**
	 * Generate a closing boundary line with two leading hyphens, the boundary,
	 * two trailing hyphens, and CRLF.
	 * @param boundary the multipart boundary
	 * @param bufferFactory the buffer factory to use
	 * @return a publisher for the closing boundary line buffer
	 * @since 5.3
	 */
	protected Mono<DataBuffer> generateLastLine(byte[] boundary, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			DataBuffer buffer = bufferFactory.allocateBuffer(boundary.length + 6);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write(boundary);
			buffer.write((byte)'-');
			buffer.write((byte)'-');
			buffer.write((byte)'\r');
			buffer.write((byte)'\n');
			return buffer;
		});
	}

	/**
	 * Generate the part headers followed by an empty line.
	 * <p>Encode header names and values using {@link #getCharset()}, terminate
	 * each header line with CRLF, and append a final CRLF to separate the
	 * headers from the body.
	 * @param headers the part headers to write
	 * @param bufferFactory the buffer factory to use
	 * @return a publisher for the encoded headers buffer
	 * @since 5.3
	 */
	protected Mono<DataBuffer> generatePartHeaders(HttpHeaders headers, DataBufferFactory bufferFactory) {
		return Mono.fromCallable(() -> {
			@SuppressWarnings("resource")
			FastByteArrayOutputStream bos = new FastByteArrayOutputStream();
			for (Map.Entry<String, List<String>> entry : headers.headerSet()) {
				byte[] headerName = entry.getKey().getBytes(getCharset());
				for (String headerValueString : entry.getValue()) {
					byte[] headerValue = headerValueString.getBytes(getCharset());
					bos.write(headerName);
					bos.write((byte)':');
					bos.write((byte)' ');
					bos.write(headerValue);
					bos.write((byte)'\r');
					bos.write((byte)'\n');
				}
			}
			bos.write((byte)'\r');
			bos.write((byte)'\n');

			byte[] bytes = bos.toByteArrayUnsafe();
			return bufferFactory.wrap(bytes);
		});
	}

}
