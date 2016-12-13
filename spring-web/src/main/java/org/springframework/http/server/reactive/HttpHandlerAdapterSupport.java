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
package org.springframework.http.server.reactive;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for adapters from native runtime HTTP request handlers to a
 * reactive {@link HttpHandler} contract.
 *
 * <p>Provides support for delegating incoming requests to a single or multiple
 * {@link HttpHandler}s each mapped to a distinct context path. In either case
 * sub-classes simply use {@link #getHttpHandler()} to access the handler to
 * delegate incoming requests to.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public abstract class HttpHandlerAdapterSupport {

	protected final Log logger = LogFactory.getLog(getClass());

	private final HttpHandler httpHandler;


	/**
	 * Constructor with a single {@code HttpHandler} to use for all requests.
	 * @param httpHandler the handler to use
	 */
	public HttpHandlerAdapterSupport(HttpHandler httpHandler) {
		Assert.notNull(httpHandler, "'httpHandler' is required");
		this.httpHandler = httpHandler;
	}

	/**
	 * Constructor with {@code HttpHandler}s mapped to distinct context paths.
	 * Context paths must start but not end with "/" and must be encoded.
	 *
	 * <p>At request time context paths are compared against the "raw" path of
	 * the request URI in the order in which they are provided. The first one
	 * to match is chosen. If none match the response status is set to 404.
	 *
	 * @param handlerMap map with context paths and {@code HttpHandler}s.
	 * @see ServerHttpRequest#getContextPath()
	 */
	public HttpHandlerAdapterSupport(Map<String, HttpHandler> handlerMap) {
		this.httpHandler = new CompositeHttpHandler(handlerMap);
	}


	/**
	 * Return the {@link HttpHandler} to delegate incoming requests to.
	 */
	public HttpHandler getHttpHandler() {
		return this.httpHandler;
	}


	/**
	 * Composite HttpHandler that selects the handler to use by context path.
	 */
	private static class CompositeHttpHandler implements HttpHandler {

		private final Map<String, HttpHandler> handlerMap;


		public CompositeHttpHandler(Map<String, HttpHandler> handlerMap) {
			Assert.notEmpty(handlerMap);
			this.handlerMap = initHandlerMap(handlerMap);
		}

		private static Map<String, HttpHandler> initHandlerMap(Map<String, HttpHandler> inputMap) {
			inputMap.keySet().stream().forEach(CompositeHttpHandler::validateContextPath);
			return new LinkedHashMap<>(inputMap);
		}

		private static void validateContextPath(String contextPath) {
			Assert.hasText(contextPath, "contextPath must not be empty");
			if (!contextPath.equals("/")) {
				Assert.isTrue(contextPath.startsWith("/"), "contextPath must begin with '/'");
				Assert.isTrue(!contextPath.endsWith("/"), "contextPath must not end with '/'");
			}
		}

		@Override
		public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
			String path = getPathToUse(request);
			return this.handlerMap.entrySet().stream()
					.filter(entry -> path.startsWith(entry.getKey()))
					.findFirst()
					.map(entry -> {
						// Preserve "native" contextPath from underlying request..
						String contextPath = request.getContextPath() + entry.getKey();
						ServerHttpRequest mutatedRequest = request.mutate().contextPath(contextPath).build();
						HttpHandler handler = entry.getValue();
						return handler.handle(mutatedRequest, response);
					})
					.orElseGet(() -> {
						response.setStatusCode(HttpStatus.NOT_FOUND);
						response.setComplete();
						return Mono.empty();
					});
		}

		/** Strip the context path from native request if any */
		private String getPathToUse(ServerHttpRequest request) {
			String path = request.getURI().getRawPath();
			String contextPath = request.getContextPath();
			if (!StringUtils.hasText(contextPath)) {
				return path;
			}
			int contextLength = contextPath.length();
			return (path.length() > contextLength ? path.substring(contextLength) : "");
		}
	}

}
