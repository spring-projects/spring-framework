/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.webjars.WebJarVersionLocator;

import org.springframework.core.io.Resource;

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
 * @see org.springframework.web.servlet.config.annotation.ResourceChainRegistration
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
	protected @Nullable Resource resolveResourceInternal(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		Resource resolved = chain.resolveResource(request, requestPath, locations);
		if (resolved == null) {
			String webJarResourcePath = findWebJarResourcePath(requestPath);
			if (webJarResourcePath != null) {
				return chain.resolveResource(request, webJarResourcePath, locations);
			}
		}
		return resolved;
	}

	@Override
	protected @Nullable String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String path = chain.resolveUrlPath(resourceUrlPath, locations);
		if (path == null) {
			String webJarResourcePath = findWebJarResourcePath(resourceUrlPath);
			if (webJarResourcePath != null) {
				return chain.resolveUrlPath(webJarResourcePath, locations);
			}
		}
		return path;
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
