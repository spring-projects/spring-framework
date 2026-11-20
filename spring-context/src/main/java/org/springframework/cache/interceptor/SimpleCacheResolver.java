/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.util.Collection;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Contract;

/**
 * A simple {@link CacheResolver} that resolves the {@link Cache} instance(s)
 * based on a configurable {@link CacheManager} and the name of the
 * cache(s) as provided by {@link BasicOperation#getCacheNames() getCacheNames()}.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see BasicOperation#getCacheNames()
 */
public class SimpleCacheResolver extends AbstractCacheResolver {

	/**
	 * Construct a new {@code SimpleCacheResolver}.
	 * @see #setCacheManager
	 */
	public SimpleCacheResolver() {
	}

	/**
	 * Construct a new {@code SimpleCacheResolver} for the given {@link CacheManager}.
	 * @param cacheManager the CacheManager to use
	 */
	public SimpleCacheResolver(CacheManager cacheManager) {
		super(cacheManager);
	}


	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return context.getOperation().getCacheNames();
	}


	/**
	 * Return a {@code SimpleCacheResolver} for the given {@link CacheManager}.
	 * @param cacheManager the CacheManager (potentially {@code null})
	 * @return the SimpleCacheResolver ({@code null} if the CacheManager was {@code null})
	 * @since 5.1
	 */
	@Contract("null -> null; !null -> !null")
	static @Nullable SimpleCacheResolver of(@Nullable CacheManager cacheManager) {
		return (cacheManager != null ? new SimpleCacheResolver(cacheManager) : null);
	}

}
