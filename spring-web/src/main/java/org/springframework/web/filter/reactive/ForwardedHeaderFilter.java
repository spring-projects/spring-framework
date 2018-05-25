/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.Set;

import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.Nullable;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers, and use them to
 * override {@link ServerHttpRequest#getURI()} to reflect the client-originated
 * protocol and address.
 *
 * <p>This filter can also be used in a {@link #setRemoveOnly removeOnly} mode
 * where "Forwarded" and "X-Forwarded-*" headers are eliminated, and not used.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 */
public class ForwardedHeaderFilter implements WebFilter {

	private static final Set<String> FORWARDED_HEADER_NAMES = new LinkedHashSet<>(5);

	static {
		FORWARDED_HEADER_NAMES.add("Forwarded");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Host");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Port");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Proto");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Prefix");
		FORWARDED_HEADER_NAMES.add("X-Forwarded-Ssl");
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

		ServerWebExchange mutatedExchange;

		if (this.removeOnly) {
			mutatedExchange = exchange.mutate().request(builder ->
					builder.headers(headers -> {
						FORWARDED_HEADER_NAMES.forEach(headers::remove);
					}))
					.build();
		}
		else {
			URI uri = UriComponentsBuilder.fromHttpRequest(exchange.getRequest()).build().toUri();
			String prefix = getForwardedPrefix(exchange.getRequest().getHeaders());

			mutatedExchange = exchange.mutate().request(builder -> {
				builder.uri(uri);
				if (prefix != null) {
					builder.path(prefix + uri.getPath());
					builder.contextPath(prefix);
				}
			}).build();
		}

		return chain.filter(mutatedExchange);
	}

	private boolean shouldNotFilter(ServerHttpRequest request) {
		HttpHeaders headers = request.getHeaders();
		for (String headerName : FORWARDED_HEADER_NAMES) {
			if (headers.containsKey(headerName)) {
				return false;
			}
		}
		return true;
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
