/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.server.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;

/**
 * Default implementation of {@link WebFilterChain}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultWebFilterChain implements WebFilterChain {

	private final List<WebFilter> filters;

	private final WebHandler handler;

	private final int index;


	public DefaultWebFilterChain(WebHandler handler, WebFilter... filters) {
		Assert.notNull(handler, "WebHandler is required");
		this.filters = ObjectUtils.isEmpty(filters) ? Collections.emptyList() : Arrays.asList(filters);
		this.handler = handler;
		this.index = 0;
	}

	private DefaultWebFilterChain(DefaultWebFilterChain parent, int index) {
		this.filters = parent.getFilters();
		this.handler = parent.getHandler();
		this.index = index;
	}


	public List<WebFilter> getFilters() {
		return this.filters;
	}

	public WebHandler getHandler() {
		return this.handler;
	}


	@Override
	public Mono<Void> filter(ServerWebExchange exchange) {
		return Mono.defer(() -> {
			if (this.index < this.filters.size()) {
				WebFilter filter = this.filters.get(this.index);
				WebFilterChain chain = new DefaultWebFilterChain(this, this.index + 1);
				return filter.filter(exchange, chain);
			}
			else {
				return this.handler.handle(exchange);
			}
		});
	}

}
