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

package org.springframework.sockjs.server.support;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.http.server.AsyncServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.sockjs.SockJsHandler;
import org.springframework.sockjs.server.SockJsService;
import org.springframework.util.Assert;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.util.NestedServletException;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.websocket.HandlerProvider;


/**
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsHttpRequestHandler implements HttpRequestHandler, BeanFactoryAware {

	private final String prefix;

	private final SockJsService sockJsService;

	private final HandlerProvider<SockJsHandler> handlerProvider;

	private final UrlPathHelper urlPathHelper = new UrlPathHelper();


	/**
	 * Class constructor with {@link SockJsHandler} instance ...
	 *
	 * @param prefix the path prefix for the SockJS service. All requests with a path
	 * that begins with the specified prefix will be handled by this service. In a
	 * Servlet container this is the path within the current servlet mapping.
	 */
	public SockJsHttpRequestHandler(String prefix, SockJsService sockJsService, SockJsHandler sockJsHandler) {

		Assert.hasText(prefix, "prefix is required");
		Assert.notNull(sockJsService, "sockJsService is required");
		Assert.notNull(sockJsHandler, "sockJsHandler is required");

		this.prefix = prefix;
		this.sockJsService = sockJsService;
		this.sockJsService.registerSockJsHandlers(Collections.singleton(sockJsHandler));
		this.handlerProvider = new HandlerProvider<SockJsHandler>(sockJsHandler);
	}

	/**
	 * Class constructor with {@link SockJsHandler} type (per request) ...
	 *
	 * @param prefix the path prefix for the SockJS service. All requests with a path
	 * that begins with the specified prefix will be handled by this service. In a
	 * Servlet container this is the path within the current servlet mapping.
	 */
	public SockJsHttpRequestHandler(String prefix, SockJsService sockJsService,
			Class<? extends SockJsHandler> sockJsHandlerClass) {

		Assert.hasText(prefix, "prefix is required");
		Assert.notNull(sockJsService, "sockJsService is required");
		Assert.notNull(sockJsHandlerClass, "sockJsHandlerClass is required");

		this.prefix = prefix;
		this.sockJsService = sockJsService;
		this.handlerProvider = new HandlerProvider<SockJsHandler>(sockJsHandlerClass);
	}

	public String getPrefix() {
		return this.prefix;
	}

	public String getPattern() {
		return this.prefix + "/**";
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.handlerProvider.setBeanFactory(beanFactory);
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String lookupPath = this.urlPathHelper.getLookupPathForRequest(request);

		Assert.isTrue(lookupPath.startsWith(this.prefix),
				"Request path does not match the prefix of the SockJsService " + this.prefix);

		String sockJsPath = lookupPath.substring(prefix.length());

		ServerHttpRequest httpRequest = new AsyncServletServerHttpRequest(request, response);
		ServerHttpResponse httpResponse = new ServletServerHttpResponse(response);

		try {
			SockJsHandler sockJsHandler = this.handlerProvider.getHandler();
			this.sockJsService.handleRequest(httpRequest, httpResponse, sockJsPath, sockJsHandler);
		}
		catch (Exception ex) {
			// TODO
			throw new NestedServletException("SockJS service failure", ex);
		}
	}

}
