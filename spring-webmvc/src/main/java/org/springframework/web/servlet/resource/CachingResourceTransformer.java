/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.web.servlet.resource.ResourceTransformer} that checks a
 * {@link org.springframework.cache.Cache} to see if a previously transformed resource
 * exists in the cache and returns it if found, and otherwise delegates to the resolver
 * chain and saves the result in the cache.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class CachingResourceTransformer implements ResourceTransformer {

	private static final Log logger = LogFactory.getLog(CachingResourceTransformer.class);

	private final Cache cache;


	public CachingResourceTransformer(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	public CachingResourceTransformer(CacheManager cacheManager, String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache == null) {
			throw new IllegalArgumentException("Cache '" + cacheName + "' not found");
		}
		this.cache = cache;
	}


	/**
	 * Return the configured {@code Cache}.
	 */
	public Cache getCache() {
		return this.cache;
	}


	@Override
	public Resource transform(HttpServletRequest request, Resource resource, ResourceTransformerChain transformerChain)
			throws IOException {

		Resource transformed = this.cache.get(resource, Resource.class);
		if (transformed != null) {
			logger.trace("Resource resolved from cache");
			return transformed;
		}

		transformed = transformerChain.transform(request, resource);
		this.cache.put(resource, transformed);

		return transformed;
	}

}
