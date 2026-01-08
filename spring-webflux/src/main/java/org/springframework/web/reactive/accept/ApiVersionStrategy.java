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
	 * Resolve the version value from a request.
	 * @param exchange the current exchange
	 * @return a {@code Mono} emitting the raw version as a {@code String},
	 * or an empty {@code Mono} if no version is found
	 * @since 7.0.3
	 * @see ApiVersionResolver
	 */
	default Mono<String> resolveApiVersion(ServerWebExchange exchange) {
		return Mono.justOrEmpty(resolveVersion(exchange));
	}

	/**
	 * Resolve the version value from a request, e.g. from a request header.
	 * @param exchange the current exchange
	 * @return the version, if present or {@code null}
	 * @see ApiVersionResolver
	 * @deprecated as of 7.0.3, in favor of {@link #resolveApiVersion(ServerWebExchange)}
	 */
	@Deprecated(since = "7.0.3", forRemoval = true)
	@Nullable
	String resolveVersion(ServerWebExchange exchange);

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
	 * Convenience method to resolve, parse, and validate the request version,
	 * or return the default version if configured.
	 * @param exchange the current exchange
	 * @return a {@code Mono} emitting the request version, a validation error,
	 * or an empty {@code Mono} if there is no version
	 * @since 7.0.3
	 */
	default Mono<Comparable<?>> resolveParseAndValidateApiVersion(ServerWebExchange exchange) {

		Mono<Comparable<?>> result = resolveApiVersion(exchange)
				.map(value -> {
					Comparable<?> version;
					try {
						version = parseVersion(value);
					}
					catch (Exception ex) {
						throw new InvalidApiVersionException(value, null, ex);
					}
					validateVersion(version, exchange);
					return version;
				});

		return result.switchIfEmpty(Mono.fromSupplier(() -> {
			Comparable<?> defaultVersion = getDefaultVersion();
			if (defaultVersion != null) {
				return defaultVersion;
			}
			else {
				validateVersion(null, exchange);
				return null;
			}
		}));
	}

	/**
	 * Convenience method to return the parsed and validated request version,
	 * or the default version if configured.
	 * @param exchange the current exchange
	 * @return the parsed request version, or the default version
	 * @deprecated in favor of {@link #resolveParseAndValidateApiVersion(ServerWebExchange)}
	 */
	@Deprecated(since = "7.0.3", forRemoval = true)
	default @Nullable Comparable<?> resolveParseAndValidateVersion(ServerWebExchange exchange) {
		String value = resolveVersion(exchange);
		Comparable<?> version;
		if (value == null) {
			version = getDefaultVersion();
		}
		else {
			try {
				version = parseVersion(value);
			}
			catch (Exception ex) {
				throw new InvalidApiVersionException(value, null, ex);
			}
		}
		validateVersion(version, exchange);
		return version;
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
