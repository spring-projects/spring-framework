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

package org.springframework.web.server.handler;

import reactor.core.publisher.Mono;

import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;

/**
 * {@link WebHandler} that decorates and delegates to another {@code WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class WebHandlerDecorator implements WebHandler {

	private final WebHandler delegate;


	/**
	 * Create a {@code WebHandlerDecorator} for the given delegate.
	 * @param delegate the WebHandler delegate
	 */
	public WebHandlerDecorator(WebHandler delegate) {
		Assert.notNull(delegate, "'delegate' must not be null");
		this.delegate = delegate;
	}


	/**
	 * Return the wrapped delegate.
	 */
	public WebHandler getDelegate() {
		return this.delegate;
	}


	@Override
	public Mono<Void> handle(ServerWebExchange exchange) {
		return this.delegate.handle(exchange);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " [delegate=" + this.delegate + "]";
	}

}
