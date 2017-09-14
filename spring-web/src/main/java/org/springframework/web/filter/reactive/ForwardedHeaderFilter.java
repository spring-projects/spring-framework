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

package org.springframework.web.filter.reactive;

import java.net.URI;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers in order to change
 * and override {@link ServerHttpRequest#getURI()}.
 * In effect the request URI will reflect the client-originated
 * protocol and address.
 *
 * <p><strong>Note:</strong> This filter can also be used in a
 * {@link #setRemoveOnly removeOnly} mode where "Forwarded" and "X-Forwarded-*"
 * headers are only eliminated without being used.
 * @author Arjen Poutsma
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 * @since 5.0
 */
public class ForwardedHeaderFilter implements WebFilter {

	private static final Set<String> FORWARDED_HEADER_NAMES =
			Collections.newSetFromMap(new LinkedCaseInsensitiveMap<>(5, Locale.ENGLISH));

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
	}

	private boolean removeOnly;

	/**
	 * Enables mode in which any "Forwarded" or "X-Forwarded-*" headers are
	 * removed only and the information in them ignored.
	 * @param removeOnly whether to discard and ignore forwarded headers
	 */
	public void setRemoveOnly(boolean removeOnly) {
		this.removeOnly = removeOnly;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

		if (shouldNotFilter(exchange.getRequest())) {
			return chain.filter(exchange);
		}

		if (this.removeOnly) {
			ServerWebExchange withoutForwardHeaders = exchange.mutate()
					.request(builder -> builder.headers(
							headers -> {
								for (String headerName : FORWARDED_HEADER_NAMES) {
									headers.remove(headerName);
								}
							})).build();
			return chain.filter(withoutForwardHeaders);
		}
		else {
			URI uri = UriComponentsBuilder.fromHttpRequest(exchange.getRequest()).build().toUri();
			String prefix = getForwardedPrefix(exchange.getRequest().getHeaders());

			ServerWebExchange withChangedUri = exchange.mutate()
					.request(builder -> {
						builder.uri(uri);
						if (prefix != null) {
							builder.path(prefix + uri.getPath());
							builder.contextPath(prefix);
						}
					}).build();
			return chain.filter(withChangedUri);
		}

	}

	private boolean shouldNotFilter(ServerHttpRequest request) {
		return request.getHeaders().keySet().stream()
				.noneMatch(FORWARDED_HEADER_NAMES::contains);
	}

	@Nullable
	private static String getForwardedPrefix(HttpHeaders headers) {
		String prefix = headers.getFirst("X-Forwarded-Prefix");
		if (prefix != null) {
			while (prefix.endsWith("/")) {
				prefix = prefix.substring(0, prefix.length() - 1);
			}
		}
		return prefix;
	}

}
