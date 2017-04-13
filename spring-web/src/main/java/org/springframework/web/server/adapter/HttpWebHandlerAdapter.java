/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.server.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.WebHandlerDecorator;
import org.springframework.web.server.session.DefaultWebSessionManager;
import org.springframework.web.server.session.WebSessionManager;

/**
 * Default adapter of {@link WebHandler} to the {@link HttpHandler} contract.
 *
 * <p>By default creates and configures a {@link DefaultServerWebExchange} and
 * then invokes the target {@code WebHandler}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpWebHandlerAdapter extends WebHandlerDecorator implements HttpHandler {

	private static final Log logger = LogFactory.getLog(HttpWebHandlerAdapter.class);

	private WebSessionManager sessionManager = new DefaultWebSessionManager();


	public HttpWebHandlerAdapter(WebHandler delegate) {
		super(delegate);
	}


	/**
	 * Configure a custom {@link WebSessionManager} to use for managing web
	 * sessions. The provided instance is set on each created
	 * {@link DefaultServerWebExchange}.
	 * <p>By default this is set to {@link DefaultWebSessionManager}.
	 * @param sessionManager the session manager to use
	 */
	public void setSessionManager(WebSessionManager sessionManager) {
		Assert.notNull(sessionManager, "WebSessionManager must not be null");
		this.sessionManager = sessionManager;
	}

	/**
	 * Return the configured {@link WebSessionManager}.
	 */
	public WebSessionManager getSessionManager() {
		return this.sessionManager;
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		ServerWebExchange exchange = createExchange(request, response);
		return getDelegate().handle(exchange)
				.switchOnError(ex -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not complete request", ex);
					}
					response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
					return Mono.empty();
				})
				.then(response::setComplete);
	}

	protected ServerWebExchange createExchange(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultServerWebExchange(request, response, this.sessionManager);
	}

}
