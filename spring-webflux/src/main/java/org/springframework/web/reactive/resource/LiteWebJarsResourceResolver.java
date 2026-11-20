/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.webjars.WebJarVersionLocator;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@code ResourceResolver} that delegates to the chain to locate a resource and then
 * attempts to find a matching versioned resource contained in a WebJar JAR file.
 *
 * <p>This allows WebJars.org users to write version agnostic paths in their templates,
 * like {@code <script src="/webjars/jquery/jquery.min.js"/>}.
 * This path will be resolved to the unique version {@code <script src="/webjars/jquery/1.2.0/jquery.min.js"/>},
 * which is a better fit for HTTP caching and version management in applications.
 *
 * <p>This also resolves resources for version agnostic HTTP requests {@code "GET /jquery/jquery.min.js"}.
 *
 * <p>This resolver requires the {@code org.webjars:webjars-locator-lite} library
 * on the classpath and is automatically registered if that library is present.
 *
 * @author Sebastien Deleuze
 * @since 6.2
 * @see <a href="https://www.webjars.org">webjars.org</a>
 */
public class LiteWebJarsResourceResolver extends AbstractResourceResolver {

	private static final int WEBJARS_LOCATION_LENGTH = WebJarVersionLocator.WEBJARS_PATH_PREFIX.length() + 1;

	private final WebJarVersionLocator webJarVersionLocator;


	/**
	 * Create a {@code LiteWebJarsResourceResolver} with a default {@code WebJarVersionLocator} instance.
	 */
	public LiteWebJarsResourceResolver() {
		this.webJarVersionLocator = new WebJarVersionLocator();
	}

	/**
	 * Create a {@code LiteWebJarsResourceResolver} with a custom {@code WebJarVersionLocator} instance,
	 * for example, with a custom cache implementation.
	 */
	public LiteWebJarsResourceResolver(WebJarVersionLocator webJarVersionLocator) {
		this.webJarVersionLocator = webJarVersionLocator;
	}


	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(exchange, requestPath, locations)
				.switchIfEmpty(Mono.defer(() -> {
					String webJarsResourcePath = findWebJarResourcePath(requestPath);
					if (webJarsResourcePath != null) {
						return chain.resolveResource(exchange, webJarsResourcePath, locations);
					}
					else {
						return Mono.empty();
					}
				}));
	}

	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveUrlPath(resourceUrlPath, locations)
				.switchIfEmpty(Mono.defer(() -> {
					String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
					if (webJarResourcePath != null) {
						Mono<String> fallback = (webJarResourcePath.endsWith("/")) ? Mono.just(webJarResourcePath) : Mono.empty();
						return chain.resolveUrlPath(webJarResourcePath, locations).switchIfEmpty(fallback);
					}
					else {
						return Mono.empty();
					}
				}));
	}

	protected @Nullable String findWebJarResourcePath(String path) {
		int endOffset = path.indexOf('/', 1);
		if (endOffset != -1) {
			int startOffset = (path.startsWith("/") ? 1 : 0);
			String webjar = path.substring(startOffset, endOffset);
			String partialPath = path.substring(endOffset + 1);
			String webJarPath = this.webJarVersionLocator.fullPath(webjar, partialPath);
			if (webJarPath != null) {
				return webJarPath.substring(WEBJARS_LOCATION_LENGTH);
			}
		}
		return null;
	}

}
