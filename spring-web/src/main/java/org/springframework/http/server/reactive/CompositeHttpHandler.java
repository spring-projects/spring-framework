
package org.springframework.http.server.reactive;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

/**
 * Composite HttpHandler that selects the handler to use by context path.
 *
 * @author Rossen Stoyanchev
 */
class CompositeHttpHandler implements HttpHandler {

	private final Map<String, HttpHandler> handlerMap;

	public CompositeHttpHandler(Map<String, ? extends HttpHandler> handlerMap) {
		Assert.notEmpty(handlerMap, "Handler map must not be empty");
		this.handlerMap = initHandlerMap(handlerMap);
	}

	private static Map<String, HttpHandler> initHandlerMap(
			Map<String, ? extends HttpHandler> inputMap) {
		inputMap.keySet().stream().forEach(CompositeHttpHandler::validateContextPath);
		return new LinkedHashMap<>(inputMap);
	}

	private static void validateContextPath(String contextPath) {
		Assert.hasText(contextPath, "Context path must not be empty");
		if (!contextPath.equals("/")) {
			Assert.isTrue(contextPath.startsWith("/"),
					"Context path must begin with '/'");
			Assert.isTrue(!contextPath.endsWith("/"),
					"Context path must not end with '/'");
		}
	}

	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		String path = getPathToUse(request);
		return this.handlerMap.entrySet().stream().filter(
				entry -> path.startsWith(entry.getKey())).findFirst().map(entry -> {
					// Preserve "native" contextPath from underlying request..
					String contextPath = request.getContextPath() + entry.getKey();
					ServerHttpRequest mutatedRequest = request.mutate().contextPath(
							contextPath).build();
					HttpHandler handler = entry.getValue();
					return handler.handle(mutatedRequest, response);
				}).orElseGet(() -> {
					response.setStatusCode(HttpStatus.NOT_FOUND);
					response.setComplete();
					return Mono.empty();
				});
	}

	/**
	 * Strip the context path from the native request, if any.
	 */
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
