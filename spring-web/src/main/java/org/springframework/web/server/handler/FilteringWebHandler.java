/*
 * Copyright 2002-2018 the original author or authors.
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
import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebHandler;

/**
 * {@link WebHandler} decorator that invokes a chain of {@link WebFilter}s
 * before the delegate {@link WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class FilteringWebHandler extends WebHandlerDecorator {

	private final WebFilter[] filters;


	/**
	 * Constructor.
	 * @param filters the chain of filters
	 */
	public FilteringWebHandler(WebHandler webHandler, List<WebFilter> filters) {
		super(webHandler);
		this.filters = filters.toArray(new WebFilter[0]);
	}


	/**
	 * Return a read-only list of the configured filters.
	 */
	public List<WebFilter> getFilters() {
		return Arrays.asList(this.filters);
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return this.filters.length != 0 ?
				new DefaultWebFilterChain(getDelegate(), this.filters).filter(exchange) :
				super.handle(exchange);
	}

}
