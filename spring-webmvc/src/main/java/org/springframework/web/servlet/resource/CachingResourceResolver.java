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

import org.springframework.cache.Cache;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * A {@link org.springframework.web.servlet.resource.ResourceResolver} that
 * resolves resources from a {@link org.springframework.cache.Cache} or otherwise
 * delegates to the resolver chain and saves the result in the cache.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class CachingResourceResolver extends AbstractResourceResolver {

	private static final String REQUEST_PATH_PREFIX = "requestPath:";

	private static final String RESOURCE_URL_PATH_PREFIX = "resourceUrlPath:";


	private final Cache cache;


	public CachingResourceResolver(Cache cache) {
		Assert.notNull(cache, "'cache' is required");
		this.cache = cache;
	}

	/**
	 * Return the configured {@code Cache}.
	 */
	public Cache getCache() {
		return this.cache;
	}

	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = REQUEST_PATH_PREFIX + requestPath;
		Resource resource = this.cache.get(key, Resource.class);

		if (resource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found match");
			}
			return resource;
		}

		resource = chain.resolveResource(request, requestPath, locations);
		if (resource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Putting resolved resource in cache");
			}
			this.cache.put(key, resource);
		}

		return resource;
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = RESOURCE_URL_PATH_PREFIX + resourceUrlPath;
		String resolvedUrlPath = this.cache.get(key, String.class);

		if (resolvedUrlPath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found match");
			}
			return resolvedUrlPath;
		}

		resolvedUrlPath = chain.resolveUrlPath(resourceUrlPath, locations);
		if (resolvedUrlPath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Putting resolved resource URL path in cache");
			}
			this.cache.put(key, resolvedUrlPath);
		}

		return resolvedUrlPath;
	}

}
