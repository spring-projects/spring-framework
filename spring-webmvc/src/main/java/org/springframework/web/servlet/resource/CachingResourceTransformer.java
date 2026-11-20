/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
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
