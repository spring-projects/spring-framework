/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.webjars.MultipleMatchesException;
import org.webjars.WebJarAssetLocator;

import org.springframework.core.io.Resource;

/**
 * A {@code ResourceResolver} that delegates to the chain to locate a resource
 * and then attempts to find a matching versioned resource contained in a WebJar JAR file.
 *
 * <p>This allows WabJar users to use version-less paths in their templates, like {@code "/jquery/jquery.min.js"}
 * while this path is resolved to the unique version {@code "/jquery/1.2.0/jquery.min.js"}, which is a better fit
 * for HTTP caching and version management in applications.
 *
 * <p>This resolver requires the "org.webjars:webjars-locator" library on classpath, and is automatically
 * registered if that library is present.
 *
 * @author Brian Clozel
 * @since 4.2
 * @see <a href="http://www.webjars.org">webjars.org</a>
 */
public class WebJarsResourceResolver extends AbstractResourceResolver {

	private final static String WEBJARS_LOCATION = "META-INF/resources/webjars";

	private final static int WEBJARS_LOCATION_LENGTH = WEBJARS_LOCATION.length();

	private final WebJarAssetLocator webJarAssetLocator;

	public WebJarsResourceResolver() {
		this.webJarAssetLocator = new WebJarAssetLocator();
	}

	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return chain.resolveResource(request, requestPath, locations);
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String path = chain.resolveUrlPath(resourceUrlPath, locations);
		if (path == null) {
			try {
				int startOffset = resourceUrlPath.startsWith("/") ? 1 : 0;
				int endOffset = resourceUrlPath.indexOf("/", 1);
				if (endOffset != -1) {
					String webjar = resourceUrlPath.substring(startOffset, endOffset);
					String partialPath = resourceUrlPath.substring(endOffset);
					String webJarPath = webJarAssetLocator.getFullPath(webjar, partialPath);
					return chain.resolveUrlPath(webJarPath.substring(WEBJARS_LOCATION_LENGTH), locations);
				}
			}
			catch (MultipleMatchesException ex) {
				logger.warn("WebJar version conflict for \"" + resourceUrlPath + "\"", ex);
			}
			catch (IllegalArgumentException ex) {
				if (logger.isTraceEnabled()) {
					logger.trace("No WebJar resource found for \"" + resourceUrlPath + "\"");
				}
			}
		}
		return path;
	}

}
