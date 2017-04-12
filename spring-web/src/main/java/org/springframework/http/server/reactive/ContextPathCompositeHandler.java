
package org.springframework.http.server.reactive;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import reactor.core.publisher.Mono;

/**
 * {@code HttpHandler} delegating requests to one of several {@code HttpHandler}'s
 * based on simple, prefix-based mappings.
 *
 * <p>This is intended as a coarse-grained mechanism for delegating requests to
 * one of several applications -- each represented by an {@code HttpHandler}, with
 * the application "context path" (the prefix-based mapping) exposed via
 * {@link ServerHttpRequest#getContextPath()}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ContextPathCompositeHandler implements HttpHandler {

	private final Map<String, HttpHandler> handlerMap;


	public ContextPathCompositeHandler(Map<String, ? extends HttpHandler> handlerMap) {
		Assert.notEmpty(handlerMap, "Handler map must not be empty");
		this.handlerMap = initHandlers(handlerMap);
	}

	private static Map<String, HttpHandler> initHandlers(Map<String, ? extends HttpHandler> map) {
		map.keySet().forEach(ContextPathCompositeHandler::assertValidContextPath);
		return new LinkedHashMap<>(map);
	}

	private static void assertValidContextPath(String contextPath) {
		Assert.hasText(contextPath, "Context path must not be empty");
		if (contextPath.equals("/")) {
			return;
		}
		Assert.isTrue(contextPath.startsWith("/"), "Context path must begin with '/'");
		Assert.isTrue(!contextPath.endsWith("/"), "Context path must not end with '/'");
	}


	@Override
	public Mono<Void> handle(ServerHttpRequest request, ServerHttpResponse response) {
		String path = getPathWithinApplication(request);
		return this.handlerMap.entrySet().stream()
				.filter(entry -> path.startsWith(entry.getKey()))
				.findFirst()
				.map(entry -> {
					String contextPath = request.getContextPath() + entry.getKey();
					ServerHttpRequest newRequest = request.mutate().contextPath(contextPath).build();
					return entry.getValue().handle(newRequest, response);
				})
				.orElseGet(() -> {
					response.setStatusCode(HttpStatus.NOT_FOUND);
					response.setComplete();
					return Mono.empty();
				});
	}

	/**
	 * Get the path within the "native" context path of the underlying server,
	 * for example when running on a Servlet container.
	 */
	private String getPathWithinApplication(ServerHttpRequest request) {
		String path = request.getURI().getRawPath();
		String contextPath = request.getContextPath();
		if (!StringUtils.hasText(contextPath)) {
			return path;
		}
		int length = contextPath.length();
		return (path.length() > length ? path.substring(length) : "");
	}

}
