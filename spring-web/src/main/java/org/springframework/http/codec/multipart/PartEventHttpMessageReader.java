/*
 * Copyright 2002-2024 the original author or authors.
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
import org.springframework.http.codec.multipart.MultipartParser.HeadersToken;
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

	private int maxParts = -1;

	private long maxPartSize = -1;

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
	 * Specify the maximum number of parts allowed in a given multipart request.
	 * <p>By default this is set to -1, meaning that there is no maximum.
	 * @since 6.1
	 */
	public void setMaxParts(int maxParts) {
		this.maxParts = maxParts;
	}

	/**
	 * Configure the maximum size allowed for any part.
	 * <p>By default this is set to -1, meaning that there is no maximum.
	 * @since 6.1
	 */
	public void setMaxPartSize(long maxPartSize) {
		this.maxPartSize = maxPartSize;
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
			Flux<MultipartParser.Token> allPartsTokens = MultipartParser.parse(message.getBody(), boundary,
					this.maxHeadersSize, this.headersCharset);

			AtomicInteger partCount = new AtomicInteger();
			return allPartsTokens
					.windowUntil(HeadersToken.class::isInstance, true)
					.concatMap(partTokens -> partTokens
							.switchOnFirst((signal, flux) -> {
								if (!signal.hasValue()) {
									// complete or error signal
									return flux.cast(PartEvent.class);
								}
								else if (tooManyParts(partCount)) {
									return Mono.error(new DecodingException("Too many parts (" + partCount.get() +
											"/" + this.maxParts + " allowed)"));
								}
								MultipartParser.HeadersToken headersToken = (MultipartParser.HeadersToken) signal.get();
								Assert.state(headersToken != null, "Signal should be headers token");

								HttpHeaders headers = headersToken.headers();
								return createEvents(headers, flux.ofType(MultipartParser.BodyToken.class));
							}));
		});
	}

	private boolean tooManyParts(AtomicInteger partCount) {
		int count = partCount.incrementAndGet();
		return this.maxParts > 0 && count > this.maxParts;
	}


	private Publisher<? extends PartEvent> createEvents(HttpHeaders headers, Flux<MultipartParser.BodyToken> bodyTokens) {
		if (MultipartUtils.isFormField(headers)) {
			Flux<DataBuffer> contents = bodyTokens.map(MultipartParser.BodyToken::buffer);
			int maxSize;
			if (this.maxPartSize == -1) {
				maxSize = this.maxInMemorySize;
			}
			else {
				// maxInMemorySize is an int, so we can safely cast the long result of Math.min
				maxSize = (int) Math.min(this.maxInMemorySize, this.maxPartSize);
			}
			return DataBufferUtils.join(contents, maxSize)
					.map(content -> {
						String value = content.toString(MultipartUtils.charset(headers));
						DataBufferUtils.release(content);
						return DefaultPartEvents.form(headers, value);
					})
					.switchIfEmpty(Mono.fromCallable(() -> DefaultPartEvents.form(headers)));
		}
		else {
			boolean isFilePart = headers.getContentDisposition().getFilename() != null;
			AtomicLong partSize = new AtomicLong();
			return bodyTokens
					.concatMap(body -> {
						DataBuffer buffer = body.buffer();
						if (tooLarge(partSize, buffer)) {
							DataBufferUtils.release(buffer);
							return Mono.error(new DataBufferLimitException("Part exceeded the limit of " +
									this.maxPartSize + " bytes"));
						}
						else {
							return isFilePart ? Mono.just(DefaultPartEvents.file(headers, buffer, body.isLast()))
									: Mono.just(DefaultPartEvents.create(headers, body.buffer(), body.isLast()));
						}
					})
					.switchIfEmpty(Mono.fromCallable(() ->
							isFilePart ? DefaultPartEvents.file(headers) : DefaultPartEvents.create(headers)));
		}
	}

	private boolean tooLarge(AtomicLong partSize, DataBuffer buffer) {
		if (this.maxPartSize != -1) {
			long size = partSize.addAndGet(buffer.readableByteCount());
			return size > this.maxPartSize;
		}
		else {
			return false;
		}
	}

}
