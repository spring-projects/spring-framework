/*
 * Copyright 2002-2015 the original author or authors.
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
package org.springframework.web.server;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import reactor.Mono;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * {@link HttpHandler} that delegates to a chain of {@link HttpFilter}s followed
 * by a target {@link HttpHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class FilterChainHttpHandler extends HttpHandlerDecorator {

	private final List<HttpFilter> filters;


	public FilterChainHttpHandler(HttpHandler targetHandler, HttpFilter... filters) {
		super(targetHandler);
		this.filters = (filters != null ? Arrays.asList(filters) : Collections.emptyList());
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultHttpFilterChain().filter(request, response);
	}


	private class DefaultHttpFilterChain implements HttpFilterChain {

		private int index;

		@Override
		public Mono<Void> filter(ServerHttpRequest request, ServerHttpResponse response) {
			if (this.index < filters.size()) {
				HttpFilter filter = filters.get(this.index++);
				return filter.filter(request, response, this);
			}
			else {
				return getDelegate().handle(request, response);
			}
		}
	}

}
