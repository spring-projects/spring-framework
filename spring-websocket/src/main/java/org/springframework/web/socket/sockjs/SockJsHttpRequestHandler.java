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

	private final WebSocketHandler webSocketHandler;


	/**
	 * Create a new {@link SockJsHttpRequestHandler}.
	 * @param sockJsService the SockJS service
	 * @param webSocketHandler the websocket handler
	 */
	public SockJsHttpRequestHandler(SockJsService sockJsService, WebSocketHandler webSocketHandler) {
		Assert.notNull(sockJsService, "sockJsService must not be null");
		Assert.notNull(webSocketHandler, "webSocketHandler must not be null");
		this.sockJsService = sockJsService;
		this.webSocketHandler = decorateWebSocketHandler(webSocketHandler);
	}


	/**
	 * Decorate the WebSocketHandler provided to the class constructor.
	 *
	 * <p>By default {@link ExceptionWebSocketHandlerDecorator} and
	 * {@link LoggingWebSocketHandlerDecorator} are applied are added.
	 */
	protected WebSocketHandler decorateWebSocketHandler(WebSocketHandler handler) {
		handler = new ExceptionWebSocketHandlerDecorator(handler);
		return new LoggingWebSocketHandlerDecorator(handler);
	}

	@Override
	public void handleRequest(HttpServletRequest servletRequest, HttpServletResponse servletResponse)
			throws ServletException, IOException {

		ServerHttpRequest serverRequest = new ServletServerHttpRequest(servletRequest);
		ServerHttpResponse serverResponse = new ServletServerHttpResponse(servletResponse);

		this.sockJsService.handleRequest(serverRequest, serverResponse, this.webSocketHandler);
	}

}
