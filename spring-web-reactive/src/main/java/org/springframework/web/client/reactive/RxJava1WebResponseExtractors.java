/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.client.reactive;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import reactor.core.converter.RxJava1ObservableConverter;
import reactor.core.converter.RxJava1SingleConverter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import rx.Observable;
import rx.Single;

import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpResponse;

/**
 * Static factory methods for {@link WebResponseExtractor}
 * based on the {@link Observable} and {@link Single} API.
 *
 * @author Brian Clozel
 */
public class RxJava1WebResponseExtractors {

	private static final Charset UTF_8 = Charset.forName("UTF-8");

	private static final Object[] HINTS = new Object[] {UTF_8};

	/**
	 * Extract the response body and decode it, returning it as a {@code Single<T>}
	 */
	public static <T> WebResponseExtractor<Single<T>> body(Class<T> sourceClass) {

		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		//noinspection unchecked
		return webResponse -> (Single<T>) RxJava1SingleConverter.from(webResponse.getClientResponse()
				.flatMap(resp -> decodeResponseBody(resp, resolvableType, webResponse.getMessageDecoders()))
				.next());
	}

	/**
	 * Extract the response body and decode it, returning it as an {@code Observable<T>}
	 */
	public static <T> WebResponseExtractor<Observable<T>> bodyStream(Class<T> sourceClass) {

		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return webResponse -> RxJava1ObservableConverter.from(webResponse.getClientResponse()
				.flatMap(resp -> decodeResponseBody(resp, resolvableType, webResponse.getMessageDecoders())));
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as a single type {@code T}
	 */
	public static <T> WebResponseExtractor<Single<ResponseEntity<T>>> response(Class<T> sourceClass) {

		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return webResponse -> (Single<ResponseEntity<T>>)
				RxJava1SingleConverter.from(webResponse.getClientResponse()
						.then(response ->
								Mono.when(
										decodeResponseBody(response, resolvableType, webResponse.getMessageDecoders()).next(),
										Mono.just(response.getHeaders()),
										Mono.just(response.getStatusCode())))
						.map(tuple -> {
							//noinspection unchecked
							return new ResponseEntity<>((T) tuple.getT1(), tuple.getT2(), tuple.getT3());
						}));
	}

	/**
	 * Extract the full response body as a {@code ResponseEntity}
	 * with its body decoded as an {@code Observable<T>}
	 */
	public static <T> WebResponseExtractor<Single<ResponseEntity<Observable<T>>>> responseStream(Class<T> sourceClass) {
		ResolvableType resolvableType = ResolvableType.forClass(sourceClass);
		return webResponse -> RxJava1SingleConverter.from(webResponse.getClientResponse()
				.map(response -> new ResponseEntity<>(
						RxJava1ObservableConverter
								.from(decodeResponseBody(response, resolvableType, webResponse.getMessageDecoders())),
						response.getHeaders(),
						response.getStatusCode())));
	}

	/**
	 * Extract the response headers as an {@code HttpHeaders} instance
	 */
	public static WebResponseExtractor<Single<HttpHeaders>> headers() {
		return webResponse -> RxJava1SingleConverter
				.from(webResponse.getClientResponse().map(resp -> resp.getHeaders()));
	}

	protected static <T> Flux<T> decodeResponseBody(ClientHttpResponse response, ResolvableType responseType,
			List<Decoder<?>> messageDecoders) {

		MediaType contentType = response.getHeaders().getContentType();
		Optional<Decoder<?>> decoder = resolveDecoder(messageDecoders, responseType, contentType);
		if (!decoder.isPresent()) {
			return Flux.error(new IllegalStateException("Could not decode response body of type '" + contentType +
					"' with target type '" + responseType.toString() + "'"));
		}
		//noinspection unchecked
		return (Flux<T>) decoder.get().decode(response.getBody(), responseType, contentType, HINTS);
	}


	protected static Optional<Decoder<?>> resolveDecoder(List<Decoder<?>> messageDecoders, ResolvableType type,
			MediaType mediaType) {
		return messageDecoders.stream().filter(e -> e.canDecode(type, mediaType)).findFirst();
	}
}
