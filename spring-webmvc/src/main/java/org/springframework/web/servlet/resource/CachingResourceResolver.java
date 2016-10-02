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

import com.google.common.annotations.VisibleForTesting;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

/**
 * A {@link org.springframework.web.servlet.resource.ResourceResolver} that
 * resolves resources from a {@link org.springframework.cache.Cache} or otherwise
 * delegates to the resolver chain and saves the result in the cache.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 4.1
 */
public class CachingResourceResolver extends AbstractResourceResolver {

	public static final String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedResource:";

	public static final String RESOLVED_URL_PATH_CACHE_KEY_PREFIX = "resolvedUrlPath:";


	private final Cache cache;


	public CachingResourceResolver(CacheManager cacheManager, String cacheName) {
		this(cacheManager.getCache(cacheName));
	}

	public CachingResourceResolver(Cache cache) {
		Assert.notNull(cache, "Cache is required");
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

		String key = computeKey(request, requestPath);
		Resource resource = this.cache.get(key, Resource.class);

		if (resource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found match: " + resource);
			}
			return resource;
		}

		resource = chain.resolveResource(request, requestPath, locations);
		if (resource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Putting resolved resource in cache: " + resource);
			}
			this.cache.put(key, resource);
		}

		return resource;
	}

	protected String computeKey(HttpServletRequest request, String requestPath) {
		StringBuilder key = new StringBuilder(RESOLVED_RESOURCE_CACHE_KEY_PREFIX);
		key.append(requestPath);
		if (request != null) {
			String encoding = request.getHeader("Accept-Encoding");
			if (encoding != null && encoding.contains("gzip")) {
				key.append("+encoding=gzip");
			}
		}
		return key.toString();
	}

	@Override
	protected String resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String locationDigest = computeLocationDerivedDigest(locations);
		String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath + "+locationDigest=" + locationDigest;
		String resolvedUrlPath = this.cache.get(key, String.class);

		if (resolvedUrlPath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found match: \"" + resolvedUrlPath + "\"");
			}
			return resolvedUrlPath;
		}

		resolvedUrlPath = chain.resolveUrlPath(resourceUrlPath, locations);
		if (resolvedUrlPath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Putting resolved resource URL path in cache: \"" + resolvedUrlPath + "\"");
			}
			this.cache.put(key, resolvedUrlPath);
		}

		return resolvedUrlPath;
	}

	@VisibleForTesting
	protected String computeLocationDerivedDigest(List<? extends Resource> locations) {
		StringBuilder builder = new StringBuilder();
		for (Resource res : locations) {
			builder.append(res.getDescription());
			builder.append(",");
		}

		return DigestUtils.md5DigestAsHex(builder.toString().getBytes());
	}

}
