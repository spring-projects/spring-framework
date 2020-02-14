/*
 * Copyright 2002-2019 the original author or authors.
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

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DataBufferEncoder;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA;
import static org.springframework.http.codec.multipart.MultipartHttpMessageWriter.*;

/**
 * {@link HttpMessageWriter} for writing a {@code Flux<Part>}
 * as multipart form data, i.e. {@code "multipart/form-data"}, to the body
 * of a request.
 *
 * @author Sergii Karpenko
 * @since 5.2
 */
public class PartsHttpMessageWriter extends LoggingCodecSupport
		implements HttpMessageWriter<Part> {

	/**
	 * THe default charset used by the writer.
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/** Suppress logging from individual part writers (full map logged at this level). */
	private static final Map<String, Object> DEFAULT_HINTS = Hints.from(Hints.SUPPRESS_LOGGING_HINT, true);

	private final HttpMessageWriter<DataBuffer> writer;

	private Charset charset = DEFAULT_CHARSET;

	/**
	 * Constructor with a default list of part writers (String and Resource).
	 */
	public PartsHttpMessageWriter() {
		this(new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
	}

	/**
	 * Constructor with explicit list of writers for serializing parts and a
	 * writer for plain form data to fall back when no media type is specified
	 * and the actual map consists of String values only.
	 * @param writer the writer for serializing parts
	 */
	public PartsHttpMessageWriter(HttpMessageWriter<DataBuffer> writer) {
		this.writer = writer;
	}

	@Override
	public List<MediaType> getWritableMediaTypes() {
		return singletonList(MULTIPART_FORM_DATA);
	}

	@Override
	public boolean canWrite(ResolvableType elementType, @Nullable MediaType mediaType) {
		return Part.class.isAssignableFrom(elementType.toClass())
				&& MULTIPART_FORM_DATA.equals(mediaType);
	}

	@Override
	public Mono<Void> write(Publisher<? extends Part> inputStream,
							ResolvableType elementType, @Nullable MediaType mediaType, ReactiveHttpOutputMessage outputMessage,
							Map<String, Object> hints) {

		return writeMultipart(Flux.from(inputStream), outputMessage, hints);
	}

	private Mono<Void> writeMultipart(
			Flux<Part> parts, ReactiveHttpOutputMessage outputMessage, Map<String, Object> hints) {

		byte[] boundary = generateMultipartBoundary();

		Map<String, String> params = new HashMap<>(2);
		params.put("boundary", new String(boundary, StandardCharsets.US_ASCII));
		params.put("charset", getCharset().name());

		outputMessage.getHeaders().setContentType(new MediaType(MULTIPART_FORM_DATA, params));

		LogFormatUtils.traceDebug(logger, traceOn -> Hints.getLogPrefix(hints) + "Encoding " +
				(isEnableLoggingRequestDetails() ?
						LogFormatUtils.formatValue(parts, !traceOn) :
						"parts (content masked)"));

		DataBufferFactory bufferFactory = outputMessage.bufferFactory();

		Flux<DataBuffer> body = parts
				.concatMap(part -> encodePart(boundary, part, bufferFactory))
				.concatWith(generateLastLine(boundary, bufferFactory))
				.doOnDiscard(PooledDataBuffer.class, PooledDataBuffer::release);

		return outputMessage.writeWith(body);
	}

	@SuppressWarnings("unchecked")
	private <T> Flux<DataBuffer> encodePart(byte[] boundary, Part part, DataBufferFactory bufferFactory) {
		MultipartHttpMessageWriter.MultipartHttpOutputMessage outputMessage = new MultipartHttpOutputMessage(bufferFactory, getCharset());
		HttpHeaders outputHeaders = outputMessage.getHeaders();
		outputHeaders.putAll(part.headers());

		String name = part.name();
		Flux<DataBuffer> body = part.content();

		if (!outputHeaders.containsKey(HttpHeaders.CONTENT_DISPOSITION)) {
			if (part instanceof FilePart) {
				outputHeaders.setContentDispositionFormData(name, ((FilePart) part).filename());
			}
			else {
				outputHeaders.setContentDispositionFormData(name, null);
			}
		}

		MediaType contentType = outputHeaders.getContentType();

		final ResolvableType finalBodyType = ResolvableType.forClass(DataBuffer.class);

		// The writer will call MultipartHttpOutputMessage#write which doesn't actually write
		// but only stores the body Flux and returns Mono.empty().

		Mono<Void> partContentReady = writer.write(body, finalBodyType, contentType, outputMessage, DEFAULT_HINTS);

		// After partContentReady, we can access the part content from MultipartHttpOutputMessage
		// and use it for writing to the actual request body

		Flux<DataBuffer> partContent = partContentReady.thenMany(Flux.defer(outputMessage::getBody));

		return Flux.concat(
				generateBoundaryLine(boundary, bufferFactory),
				partContent,
				generateNewLine(bufferFactory));
	}

	/**
	 * Set the character set to use for part headers such as
	 * "Content-Disposition" (and its filename parameter).
	 * <p>By default this is set to "UTF-8".
	 */
	public void setCharset(Charset charset) {
		Assert.notNull(charset, "Charset must not be null");
		this.charset = charset;
	}

	/**
	 * Return the configured charset for part headers.
	 */
	public Charset getCharset() {
		return this.charset;
	}

	/**
	 * Generate a multipart boundary.
	 * <p>By default delegates to {@link MimeTypeUtils#generateMultipartBoundary()}.
	 */
	protected byte[] generateMultipartBoundary() {
		return MimeTypeUtils.generateMultipartBoundary();
	}

}
