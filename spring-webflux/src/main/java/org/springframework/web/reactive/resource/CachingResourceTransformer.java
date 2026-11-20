/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Mono;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link ResourceTransformer} that checks a {@link Cache} to see if a
 * previously transformed resource exists in the cache and returns it if found,
 * or otherwise delegates to the resolver chain and caches the result.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
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
	public Mono<Resource> transform(ServerWebExchange exchange, Resource resource,
			ResourceTransformerChain transformerChain) {

		Resource cachedResource = this.cache.get(resource, Resource.class);
		if (cachedResource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace(exchange.getLogPrefix() + "Resource resolved from cache");
			}
			return Mono.just(cachedResource);
		}

		return transformerChain.transform(exchange, resource)
				.doOnNext(transformed -> this.cache.put(resource, transformed));
	}

}
