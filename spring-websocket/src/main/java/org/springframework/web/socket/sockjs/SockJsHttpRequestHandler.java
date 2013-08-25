/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.sockjs;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.support.ExceptionWebSocketHandlerDecorator;
import org.springframework.web.socket.support.LoggingWebSocketHandlerDecorator;

/**
 * An {@link HttpRequestHandler} that allows mapping a {@link SockJsService} to requests
 * in a Servlet container.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsHttpRequestHandler implements HttpRequestHandler {

	private final SockJsService sockJsService;

	private final WebSocketHandler wsHandler;


	/**
	 * Create a new {@link SockJsHttpRequestHandler}.
	 * @param sockJsService the SockJS service
	 * @param wsHandler the websocket handler
	 */
	public SockJsHttpRequestHandler(SockJsService sockJsService, WebSocketHandler wsHandler) {
		Assert.notNull(sockJsService, "sockJsService must not be null");
		Assert.notNull(wsHandler, "webSocketHandler must not be null");
		this.sockJsService = sockJsService;
		this.wsHandler = new ExceptionWebSocketHandlerDecorator(new LoggingWebSocketHandlerDecorator(wsHandler));
	}


	@Override
	public void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		ServerHttpRequest request = new ServletServerHttpRequest(servletRequest);
		ServerHttpResponse response = new ServletServerHttpResponse(servletResponse);

		try {
			this.sockJsService.handleRequest(request, response, this.wsHandler);
		}
		catch (Throwable t) {
			throw new SockJsException("Uncaught failure in SockJS request, uri=" + request.getURI(), t);
		}
	}

}
