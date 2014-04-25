/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * A {@code ResourceResolver} configured with a prefix to be removed from the
 * request path before delegating to the chain to find a matching resource.
 *
 * <p>Enables inserting a unique virtual prefix (e.g. reduced SHA, version number,
 * release date) into resource paths so that when a new version of the application
 * is released, clients are forced to reload application resources.
 *
 * <p>This is useful when changing resource names is not an option (e.g. when
 * using JavaScript module loaders). If that's not the case, the use of
 * {@link FingerprintResourceResolver} provides more optimal performance since
 * it causes only actually modified resources to be reloaded.
 *
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 4.1
 */
public class PrefixResourceResolver extends AbstractResourceResolver {

	private final String prefix;

	public PrefixResourceResolver(String prefix) {
		Assert.hasText(prefix, "prefix must not be null or empty");
		this.prefix = prefix.startsWith("/") ? prefix.substring(1) : prefix;
	}

	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		if (requestPath.startsWith(this.prefix)) {
			requestPath = requestPath.substring(this.prefix.length());
		}

		return chain.resolveResource(request, requestPath, locations);
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		String baseUrl = chain.resolveUrlPath(resourceUrlPath, locations);
		if (StringUtils.hasText(baseUrl)) {
			return this.prefix + (baseUrl.startsWith("/") ? baseUrl : "/" + baseUrl);
		}
		return baseUrl;
	}

}
