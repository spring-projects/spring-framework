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

package org.springframework.web.reactive.accept;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.accept.MissingApiVersionException;
import org.springframework.web.server.ServerWebExchange;

/**
 * The main component that encapsulates configuration preferences and strategies
 * to manage API versioning for an application.
 *
 * @author Rossen Stoyanchev
 * @author Jonathan Kaplan
 * @since 7.0
 * @see DefaultApiVersionStrategy
 */
public interface ApiVersionStrategy {

	/**
	 * Resolve the version value from a request, e.g. from a request header.
	 * @param exchange the current exchange
	 * @return the version, if present or {@code null}
	 * @see ApiVersionResolver
	 */
	@Nullable
	String resolveVersion(ServerWebExchange exchange);

	/**
	 * Resolve the API version from the given request exchange, for example,
	 * from a request header or other context-specific source.
	 * @param exchange the current server exchange containing the request details
	 * @return a {@code Mono} emitting the resolved version as a {@code String},
	 * or completing empty if the version is not specified
	 */
	default Mono<String> resolveVersionReactively(ServerWebExchange exchange){
		return Mono.justOrEmpty(this.resolveVersion(exchange));
	}

	/**
	 * Parse the version of a request into an Object.
	 * @param version the value to parse
	 * @return an Object that represents the version
	 * @see org.springframework.web.accept.ApiVersionParser
	 */
	Comparable<?> parseVersion(String version);

	/**
	 * Validate a request version, including required and supported version checks.
	 * @param requestVersion the version to validate
	 * @param exchange the exchange
	 * @throws MissingApiVersionException if the version is required, but not specified
	 * @throws InvalidApiVersionException if the version is not supported
	 */
	void validateVersion(@Nullable Comparable<?> requestVersion, ServerWebExchange exchange)
			throws MissingApiVersionException, InvalidApiVersionException;

	/**
	 * Return a default version to use for requests that don't specify one.
	 */
	@Nullable Comparable<?> getDefaultVersion();

	/**
	 * Convenience method to return the API version from the given request exchange, parse and validate
	 * the version, and return the result as a reactive {@code Mono} stream. If no version
	 * is resolved, the default version is used.
	 * @param exchange the current server exchange containing the request details
	 * @return a {@code Mono} emitting the resolved, parsed, and validated version as a {@code Comparable<?>},
	 * or an error in case parsing or validation fails
	 */
	@SuppressWarnings("Convert2MethodRef")
	default Mono<Comparable<?>> resolveParseAndValidateVersion(ServerWebExchange exchange) {
		return this.resolveVersionReactively(exchange)
				.switchIfEmpty(Mono.justOrEmpty(this.getDefaultVersion())
									.mapNotNull(comparable -> comparable.toString()))
				.<Comparable<?>>handle((version, sink) -> {
					try {
						sink.next(this.parseVersion(version));
					}
					catch (Exception ex) {
						sink.error(new InvalidApiVersionException(version, null, ex));
					}
				}).<Comparable<?>>handle((version, sink) -> {
					try {
						this.validateVersion(version, exchange);
						sink.next(version);
					}
					catch (MissingApiVersionException | InvalidApiVersionException ex) {
						sink.error(ex);
					}
				})
				.switchIfEmpty(Mono.error(new MissingApiVersionException()));

	}

	/**
	 * Check if the requested API version is deprecated, and if so handle it
	 * accordingly, e.g. by setting response headers to signal the deprecation,
	 * to specify relevant dates and provide links to further details.
	 * @param version the resolved and parsed request version
	 * @param handler the handler chosen for the exchange
	 * @param exchange the current exchange
	 * @see ApiVersionDeprecationHandler
	 */
	void handleDeprecations(Comparable<?> version, Object handler, ServerWebExchange exchange);

}
