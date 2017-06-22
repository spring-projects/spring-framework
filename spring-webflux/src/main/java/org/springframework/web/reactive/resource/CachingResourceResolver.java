/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.List;

import reactor.core.publisher.Mono;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * A {@link ResourceResolver} that resolves resources from a {@link Cache} or
 * otherwise delegates to the resolver chain and caches the result.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public class CachingResourceResolver extends AbstractResourceResolver {

	public static final String RESOLVED_RESOURCE_CACHE_KEY_PREFIX = "resolvedResource:";

	public static final String RESOLVED_URL_PATH_CACHE_KEY_PREFIX = "resolvedUrlPath:";


	private final Cache cache;


	public CachingResourceResolver(Cache cache) {
		Assert.notNull(cache, "Cache is required");
		this.cache = cache;
	}

	public CachingResourceResolver(CacheManager cacheManager, String cacheName) {
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
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = computeKey(exchange, requestPath);
		Resource cachedResource = this.cache.get(key, Resource.class);

		if (cachedResource != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found match: " + cachedResource);
			}
			return Mono.just(cachedResource);
		}

		return chain.resolveResource(exchange, requestPath, locations)
				.doOnNext(resource -> {
					if (logger.isTraceEnabled()) {
						logger.trace("Putting resolved resource in cache: " + resource);
					}
					this.cache.put(key, resource);
				});
	}

	protected String computeKey(@Nullable ServerWebExchange exchange, String requestPath) {
		StringBuilder key = new StringBuilder(RESOLVED_RESOURCE_CACHE_KEY_PREFIX);
		key.append(requestPath);
		if (exchange != null) {
			String encoding = exchange.getRequest().getHeaders().getFirst("Accept-Encoding");
			if (encoding != null && encoding.contains("gzip")) {
				key.append("+encoding=gzip");
			}
		}
		return key.toString();
	}

	@Override
	protected Mono<String> resolveUrlPathInternal(String resourceUrlPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		String key = RESOLVED_URL_PATH_CACHE_KEY_PREFIX + resourceUrlPath;
		String cachedUrlPath = this.cache.get(key, String.class);

		if (cachedUrlPath != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Found match: \"" + cachedUrlPath + "\"");
			}
			return Mono.just(cachedUrlPath);
		}

		return chain.resolveUrlPath(resourceUrlPath, locations)
				.doOnNext(resolvedPath -> {
					if (logger.isTraceEnabled()) {
						logger.trace("Putting resolved resource URL path in cache: \"" + resolvedPath + "\"");
					}
					this.cache.put(key, resolvedPath);
				});
	}

}
