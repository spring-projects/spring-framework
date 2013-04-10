/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.sockjs.server.support;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.server.AsyncServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.sockjs.server.AbstractSockJsService;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.util.NestedServletException;

/**
 * A Spring MVC HandlerMapping for matching requests to a SockJS services based on the
 * {@link AbstractSockJsService#getPrefix() prefix} property of each service.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsServiceHandlerMapping extends AbstractHandlerMapping {

	private static Log logger = LogFactory.getLog(SockJsServiceHandlerMapping.class);

	private final List<AbstractSockJsService> sockJsServices;


	public SockJsServiceHandlerMapping(AbstractSockJsService... sockJsServices) {
		this.sockJsServices = Arrays.asList(sockJsServices);
	}

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {

		String lookupPath = getUrlPathHelper().getLookupPathForRequest(request);
		if (logger.isDebugEnabled()) {
			logger.debug("Looking for SockJS service match to path " + lookupPath);
		}

		for (AbstractSockJsService service : this.sockJsServices) {
			if (lookupPath.startsWith(service.getPrefix())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Matched to " + service);
				}
				String sockJsPath = lookupPath.substring(service.getPrefix().length());
				return new SockJsServiceHttpRequestHandler(service, sockJsPath);
			}
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Did not find a match");
		}

		return null;
	}


	/**
	 * {@link HttpRequestHandler} wrapping the invocation of the selected SockJS service.
	 */
	private static class SockJsServiceHttpRequestHandler implements HttpRequestHandler {

		private final String sockJsPath;

		private final AbstractSockJsService sockJsService;


		public SockJsServiceHttpRequestHandler(AbstractSockJsService sockJsService, String sockJsPath) {
			this.sockJsService = sockJsService;
			this.sockJsPath = sockJsPath;
		}

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {

			ServerHttpRequest httpRequest = new AsyncServletServerHttpRequest(request, response);
			ServerHttpResponse httpResponse = new ServletServerHttpResponse(response);

			try {
				this.sockJsService.handleRequest(httpRequest, httpResponse, this.sockJsPath);
			}
			catch (Exception ex) {
				// TODO
				throw new NestedServletException("SockJS service failure", ex);
			}
		}
	}

}
