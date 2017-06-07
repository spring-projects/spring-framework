/*
 * Copyright 2002-2017 the original author or authors.
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
				logger.trace("Found match: " + cachedResource);
			}
			return Mono.just(cachedResource);
		}

		return transformerChain.transform(exchange, resource)
				.doOnNext(transformed -> {
					if (logger.isTraceEnabled()) {
						logger.trace("Putting transformed resource in cache: " + transformed);
					}
					this.cache.put(resource, transformed);
				});
	}

}
