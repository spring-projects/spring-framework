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

package org.springframework.web.filter.reactive;

import reactor.core.publisher.Mono;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.adapter.ForwardedHeaderTransformer;

/**
 * Extract values from "Forwarded" and "X-Forwarded-*" headers to override the
 * request URI (i.e. {@link ServerHttpRequest#getURI()}) so it reflects the
 * client-originated protocol and address.
 *
 * <p>Alternatively if {@link #setRemoveOnly removeOnly} is set to "true", then
 * "Forwarded" and "X-Forwarded-*" headers are only removed and not used.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @deprecated as of 5.1 this filter is deprecated in favor of using
 * {@link ForwardedHeaderTransformer} which can be declared as a bean with the
 * name "forwardedHeaderTransformer" or registered explicitly in
 * {@link org.springframework.web.server.adapter.WebHttpHandlerBuilder
 * WebHttpHandlerBuilder}.
 * @since 5.0
 * @see <a href="https://tools.ietf.org/html/rfc7239">https://tools.ietf.org/html/rfc7239</a>
 */
@Deprecated
public class ForwardedHeaderFilter extends ForwardedHeaderTransformer implements WebFilter {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		if (hasForwardedHeaders(request)) {
			exchange = exchange.mutate().request(apply(request)).build();
		}
		return chain.filter(exchange);
	}

}
