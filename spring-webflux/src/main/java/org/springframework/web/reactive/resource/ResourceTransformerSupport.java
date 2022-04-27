/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.resource;

import java.util.Collections;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
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

	@Nullable
	private ResourceUrlProvider resourceUrlProvider;


	/**
	 * Configure a {@link ResourceUrlProvider} to use when resolving the public
	 * URL of links in a transformed resource (e.g. import links in a CSS file).
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
	@Nullable
	public ResourceUrlProvider getResourceUrlProvider() {
		return this.resourceUrlProvider;
	}


	/**
	 * A transformer can use this method when a resource being transformed
	 * contains links to other resources. Such links need to be replaced with the
	 * public facing link as determined by the resource resolver chain (e.g. the
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
