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
package org.springframework.http.server.reactive;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.reactivestreams.Publisher;

import org.springframework.util.Assert;

/**
 * An {@link HttpHandler} decorator that delegates to a list of
 * {@link HttpFilter}s and the target {@link HttpHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class FilterChainHttpHandler implements HttpHandler {

	private final List<HttpFilter> filters;

	private final HttpHandler targetHandler;


	public FilterChainHttpHandler(HttpHandler targetHandler, HttpFilter... filters) {
		Assert.notNull(targetHandler, "'targetHandler' is required.");
		this.filters = (filters != null ? Arrays.asList(filters) : Collections.emptyList());
		this.targetHandler = targetHandler;
	}


	@Override
	public Publisher<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultHttpFilterChain().filter(request, response);
	}


	private class DefaultHttpFilterChain implements HttpFilterChain {

		private int index;

		@Override
		public Publisher<Void> filter(ServerHttpRequest request, ServerHttpResponse response) {
			if (this.index < filters.size()) {
				HttpFilter filter = filters.get(this.index++);
				return filter.filter(request, response, this);
			}
			else {
				return targetHandler.handle(request, response);
			}
		}
	}

}
