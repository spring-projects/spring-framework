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

package org.springframework.web.reactive.function.client;

import java.net.URI;
import java.util.List;
import java.util.function.Predicate;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.codec.CodecException;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

/**
 * Internal methods shared between {@link DefaultWebClient} and
 * {@link DefaultClientResponse}.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
abstract class WebClientUtils {

	private static final String VALUE_NONE = "\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n";

	/**
	 * Predicate that returns true if an exception should be wrapped.
	 */
	public static final Predicate<? super Throwable> WRAP_EXCEPTION_PREDICATE =
			t -> !(t instanceof WebClientException) && !(t instanceof CodecException);


	/**
	 * Map the given response to a single value {@code ResponseEntity<T>}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> Mono<ResponseEntity<T>> mapToEntity(ClientResponse response, Mono<T> bodyMono) {
		return ((Mono<Object>) bodyMono).defaultIfEmpty(VALUE_NONE).map(body ->
				new ResponseEntity<>(
						body != VALUE_NONE ? (T) body : null,
						response.headers().asHttpHeaders(),
						response.statusCode()));
	}

	/**
	 * Map the given response to a {@code ResponseEntity<List<T>>}.
	 */
	public static <T> Mono<ResponseEntity<List<T>>> mapToEntityList(ClientResponse response, Publisher<T> body) {
		return Flux.from(body).collectList().map(list ->
				new ResponseEntity<>(list, response.headers().asHttpHeaders(), response.statusCode()));
	}

	/**
	 * Return a String representation of the request details for logging purposes
	 * in "METHOD URI" format.
	 * For the Security purpose, URI is returned in encoded format,
	 * while userInfo, query, and fragment is stripped out.
	 * @since 6.0.16
	 */
	public static String getRequestDescription(HttpMethod httpMethod, URI uri) {
		StringBuilder sb = new StringBuilder()
				.append(httpMethod.name()).append(" ");

		// also handles Opaque URI, which has only schemeSpecificPart
		if (uri.getRawUserInfo() == null && uri.getRawQuery() == null && uri.getRawFragment() == null) {
			return sb.append(uri).toString();
		}

		if (uri.getScheme() != null) {
			sb.append(uri.getScheme()).append(':');
		}
		if (uri.getHost() != null) {
			sb.append("//");
			String host = uri.getHost();
			// IPv6 handling
			if (host.indexOf(':') >= 0 && !host.startsWith("[") && !host.endsWith("]")) {
				sb.append('[').append(host).append(']');
			}
			else {
				sb.append(host);
			}

			if (uri.getPort() != -1) {
				sb.append(':').append(uri.getPort());
			}
		}
		if (uri.getRawPath() != null) {
			sb.append(uri.getRawPath());
		}
		return sb.toString();
	}

}
