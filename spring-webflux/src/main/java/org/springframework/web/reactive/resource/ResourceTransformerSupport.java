/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.util.Collections;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * A base class for a {@code ResourceTransformer} with an optional helper method
 * for resolving public links within a transformed resource.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class ResourceTransformerSupport implements ResourceTransformer {

	private @Nullable ResourceUrlProvider resourceUrlProvider;


	/**
	 * Configure a {@link ResourceUrlProvider} to use when resolving the public
	 * URL of links in a transformed resource (for example, import links in a CSS file).
	 * This is required only for links expressed as full paths and not for
	 * relative links.
	 * @param resourceUrlProvider the URL provider to use
	 */
	public void setResourceUrlProvider(@Nullable ResourceUrlProvider resourceUrlProvider) {
		this.resourceUrlProvider = resourceUrlProvider;
	}

	/**
	 * Return the configured {@code ResourceUrlProvider}.
	 */
	public @Nullable ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}


	/**
	 * A transformer can use this method when a resource being transformed
	 * contains links to other resources. Such links need to be replaced with the
	 * public facing link as determined by the resource resolver chain (for example, the
	 * public URL may have a version inserted).
	 * @param resourcePath the path to a resource that needs to be re-written
	 * @param exchange the current exchange
	 * @param resource the resource being transformed
	 * @param transformerChain the transformer chain
	 * @return the resolved URL or an empty {@link Mono}
	 */
	protected Mono<String> resolveUrlPath(String resourcePath, ServerWebExchange exchange,
			Resource resource, ResourceTransformerChain transformerChain) {

		if (resourcePath.startsWith("/")) {
			// full resource path
			ResourceUrlProvider urlProvider = getResourceUrlProvider();
			return (urlProvider != null ? urlProvider.getForUriString(resourcePath, exchange) : Mono.empty());
		}
		else {
			// try resolving as relative path
			return transformerChain.getResolverChain()
					.resolveUrlPath(resourcePath, Collections.singletonList(resource));
		}
	}

	/**
	 * Transform the given relative request path to an absolute path,
	 * taking the path of the given request as a point of reference.
	 * The resulting path is also cleaned from sequences like "path/..".
	 * @param path the relative path to transform
	 * @param exchange the current exchange
	 * @return the absolute request path for the given resource path
	 */
	protected String toAbsolutePath(String path, ServerWebExchange exchange) {
		String requestPath = exchange.getRequest().getURI().getPath();
		String absolutePath = (path.startsWith("/") ? path : StringUtils.applyRelativePath(requestPath, path));
		return StringUtils.cleanPath(absolutePath);
	}

}
