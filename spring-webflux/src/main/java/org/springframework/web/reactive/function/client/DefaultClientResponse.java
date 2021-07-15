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

package org.springframework.web.reactive.function.client;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.MimeType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyExtractors;

/**
 * Default implementation of {@link ClientResponse}.
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @since 5.0
 */
class DefaultClientResponse implements ClientResponse {

	private static final byte[] EMPTY = new byte[0];


	private final ClientHttpResponse response;

	private final Headers headers;

	private final ExchangeStrategies strategies;

	private final String logPrefix;

	private final String requestDescription;

	private final Supplier<HttpRequest> requestSupplier;

	private final BodyExtractor.Context bodyExtractorContext;


	public DefaultClientResponse(ClientHttpResponse response, ExchangeStrategies strategies,
			String logPrefix, String requestDescription, Supplier<HttpRequest> requestSupplier) {

		this.response = response;
		this.strategies = strategies;
		this.headers = new DefaultHeaders();
		this.logPrefix = logPrefix;
		this.requestDescription = requestDescription;
		this.requestSupplier = requestSupplier;
		this.bodyExtractorContext = new BodyExtractor.Context() {
			@Override
			public List<HttpMessageReader<?>> messageReaders() {
				return strategies.messageReaders();
			}

			@Override
			public Optional<ServerHttpResponse> serverResponse() {
				return Optional.empty();
			}

			@Override
			public Map<String, Object> hints() {
				return Hints.from(Hints.LOG_PREFIX_HINT, logPrefix);
			}
		};
	}


	@Override
	public ExchangeStrategies strategies() {
		return this.strategies;
	}

	@Override
	public HttpStatus statusCode() {
		return this.response.getStatusCode();
	}

	@Override
	public int rawStatusCode() {
		return this.response.getRawStatusCode();
	}

	@Override
	public Headers headers() {
		return this.headers;
	}

	@Override
	public MultiValueMap<String, ResponseCookie> cookies() {
		return this.response.getCookies();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T body(BodyExtractor<T, ? super ClientHttpResponse> extractor) {
		T result = extractor.extract(this.response, this.bodyExtractorContext);
		String description = "Body from " + this.requestDescription + " [DefaultClientResponse]";
		if (result instanceof Mono) {
			return (T) ((Mono<?>) result).checkpoint(description);
		}
		else if (result instanceof Flux) {
			return (T) ((Flux<?>) result).checkpoint(description);
		}
		else {
			return result;
		}
	}

	@Override
	public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
		return body(BodyExtractors.toMono(elementClass));
	}

	@Override
	public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef) {
		return body(BodyExtractors.toMono(elementTypeRef));
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
		return elementClass.equals(DataBuffer.class) ?
				(Flux<T>) body(BodyExtractors.toDataBuffers()) : body(BodyExtractors.toFlux(elementClass));
	}

	@Override
	public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef) {
		return body(BodyExtractors.toFlux(elementTypeRef));
	}

	@Override
	public Mono<Void> releaseBody() {
		return body(BodyExtractors.toDataBuffers()).map(DataBufferUtils::release).then();
	}

	@Override
	public Mono<ResponseEntity<Void>> toBodilessEntity() {
		return releaseBody().then(WebClientUtils.mapToEntity(this, Mono.empty()));
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyType) {
		return WebClientUtils.mapToEntity(this, bodyToMono(bodyType));
	}

	@Override
	public <T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference) {
		return WebClientUtils.mapToEntity(this, bodyToMono(bodyTypeReference));
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass) {
		return WebClientUtils.mapToEntityList(this, bodyToFlux(elementClass));
	}

	@Override
	public <T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef) {
		return WebClientUtils.mapToEntityList(this, bodyToFlux(elementTypeRef));
	}

	@Override
	public Mono<WebClientResponseException> createException() {
		return DataBufferUtils.join(body(BodyExtractors.toDataBuffers()))
				.map(dataBuffer -> {
					byte[] bytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(bytes);
					DataBufferUtils.release(dataBuffer);
					return bytes;
				})
				.defaultIfEmpty(EMPTY)
				.onErrorReturn(IllegalStateException.class::isInstance, EMPTY)
				.map(bodyBytes -> {
					HttpRequest request = this.requestSupplier.get();
					Charset charset = headers().contentType().map(MimeType::getCharset).orElse(null);
					int statusCode = rawStatusCode();
					HttpStatus httpStatus = HttpStatus.resolve(statusCode);
					if (httpStatus != null) {
						return WebClientResponseException.create(
								statusCode,
								httpStatus.getReasonPhrase(),
								headers().asHttpHeaders(),
								bodyBytes,
								charset,
								request);
					}
					else {
						return new UnknownHttpStatusCodeException(
								statusCode,
								headers().asHttpHeaders(),
								bodyBytes,
								charset,
								request);
					}
				});
	}

	@Override
	public String logPrefix() {
		return this.logPrefix;
	}

	// Used by DefaultClientResponseBuilder
	HttpRequest request() {
		return this.requestSupplier.get();
	}

	private class DefaultHeaders implements Headers {

		private final HttpHeaders httpHeaders =
				HttpHeaders.readOnlyHttpHeaders(response.getHeaders());

		@Override
		public OptionalLong contentLength() {
			return toOptionalLong(this.httpHeaders.getContentLength());
		}

		@Override
		public Optional<MediaType> contentType() {
			return Optional.ofNullable(this.httpHeaders.getContentType());
		}

		@Override
		public List<String> header(String headerName) {
			List<String> headerValues = this.httpHeaders.get(headerName);
			return (headerValues != null ? headerValues : Collections.emptyList());
		}

		@Override
		public HttpHeaders asHttpHeaders() {
			return this.httpHeaders;
		}

		private OptionalLong toOptionalLong(long value) {
			return (value != -1 ? OptionalLong.of(value) : OptionalLong.empty());
		}
	}

}
