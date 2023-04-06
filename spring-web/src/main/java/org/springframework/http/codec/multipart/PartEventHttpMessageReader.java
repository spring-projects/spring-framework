/*
 * Copyright 2002-2022 the original author or authors.
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpInputMessage;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.LoggingCodecSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * {@code HttpMessageReader} for parsing {@code "multipart/form-data"} requests
 * to a stream of {@link PartEvent} elements.
 *
 * @author Arjen Poutsma
 * @since 6.0
 * @see PartEvent
 */
public class PartEventHttpMessageReader extends LoggingCodecSupport implements HttpMessageReader<PartEvent> {

	private int maxInMemorySize = 256 * 1024;

	private int maxHeadersSize = 10 * 1024;

	private Charset headersCharset = StandardCharsets.UTF_8;


	/**
	 * Get the {@link #setMaxInMemorySize configured} maximum in-memory size.
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}

	/**
	 * Configure the maximum amount of memory allowed for form fields.
	 * When the limit is exceeded, form fields parts are rejected with
	 * {@link DataBufferLimitException}.

	 * <p>By default this is set to 256K.
	 * @param maxInMemorySize the in-memory limit in bytes; if set to -1 the entire
	 * contents will be stored in memory
	 */
	public void setMaxInMemorySize(int maxInMemorySize) {
		this.maxInMemorySize = maxInMemorySize;
	}

	/**
	 * Configure the maximum amount of memory that is allowed per headers section of each part.
	 * Defaults to 10K.
	 * @param byteCount the maximum amount of memory for headers
	 */
	public void setMaxHeadersSize(int byteCount) {
		this.maxHeadersSize = byteCount;
	}

	/**
	 * Set the character set used to decode headers.
	 * <p>Defaults to UTF-8 as per RFC 7578.
	 * @param headersCharset the charset to use for decoding headers
	 * @see <a href="https://tools.ietf.org/html/rfc7578#section-5.1">RFC-7578 Section 5.1</a>
	 */
	public void setHeadersCharset(Charset headersCharset) {
		Assert.notNull(headersCharset, "Charset must not be null");
		this.headersCharset = headersCharset;
	}


	@Override
	public List<MediaType> getReadableMediaTypes() {
		return Collections.singletonList(MediaType.MULTIPART_FORM_DATA);
	}

	@Override
	public boolean canRead(ResolvableType elementType, @Nullable MediaType mediaType) {
		return PartEvent.class.equals(elementType.toClass()) &&
				(mediaType == null || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(mediaType));
	}

	@Override
	public Mono<PartEvent> readMono(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {
		return Mono.error(
				new UnsupportedOperationException("Cannot read multipart request body into single PartEvent"));
	}

	@Override
	public Flux<PartEvent> read(ResolvableType elementType, ReactiveHttpInputMessage message,
			Map<String, Object> hints) {

		return Flux.defer(() -> {
			byte[] boundary = MultipartUtils.boundary(message, this.headersCharset);
			if (boundary == null) {
				return Flux.error(new DecodingException("No multipart boundary found in Content-Type: \"" +
						message.getHeaders().getContentType() + "\""));
			}
			return MultipartParser.parse(message.getBody(), boundary, this.maxHeadersSize, this.headersCharset)
					.windowUntil(t -> t instanceof MultipartParser.HeadersToken, true)
					.concatMap(tokens -> tokens.switchOnFirst((signal, flux) -> {
						if (signal.hasValue()) {
							MultipartParser.HeadersToken headersToken = (MultipartParser.HeadersToken) signal.get();
							Assert.state(headersToken != null, "Signal should be headers token");

							HttpHeaders headers = headersToken.headers();
							Flux<MultipartParser.BodyToken> bodyTokens =
									flux.filter(t -> t instanceof MultipartParser.BodyToken)
											.cast(MultipartParser.BodyToken.class);
							return createEvents(headers, bodyTokens);
						}
						else {
							// complete or error signal
							return flux.cast(PartEvent.class);
						}
					}));
		});
	}

	private Publisher<? extends PartEvent> createEvents(HttpHeaders headers, Flux<MultipartParser.BodyToken> bodyTokens) {
		if (MultipartUtils.isFormField(headers)) {
			Flux<DataBuffer> contents = bodyTokens.map(MultipartParser.BodyToken::buffer);
			return DataBufferUtils.join(contents, this.maxInMemorySize)
					.map(content -> {
						String value = content.toString(MultipartUtils.charset(headers));
						DataBufferUtils.release(content);
						return DefaultPartEvents.form(headers, value);
					})
					.switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.form(headers)));
		}
		else if (headers.getContentDisposition().getFilename() != null) {
			return bodyTokens
					.map(body -> DefaultPartEvents.file(headers, body.buffer(), body.isLast()))
					.switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.file(headers)));
		}
		else {
			return bodyTokens
					.map(body -> DefaultPartEvents.create(headers, body.buffer(), body.isLast()))
					.switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.create(headers))); // empty body
		}


	}

}
