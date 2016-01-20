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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;

/**
 * Adapt {@link WebHandler} to {@link HttpHandler} also creating the
 * {@link WebServerExchange} before invoking the target {@code WebHandler}.
 *
 * @author Rossen Stoyanchev
 */
public class WebToHttpHandlerAdapter extends WebHandlerDecorator implements HttpHandler {

	private static Log logger = LogFactory.getLog(WebToHttpHandlerAdapter.class);


	public WebToHttpHandlerAdapter(WebHandler delegate) {
		super(delegate);
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		WebServerExchange exchange = createWebServerExchange(request, response);
		return getDelegate().handle(exchange)
				.otherwise(ex -> {
					if (logger.isDebugEnabled()) {
						logger.debug("Could not complete request", ex);
					}
					response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
					return Mono.empty();
				})
				.after(response::setComplete);
	}

	protected WebServerExchange createWebServerExchange(ServerHttpRequest request, ServerHttpResponse response) {
		return new DefaultWebServerExchange(request, response);
	}

}
