/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.interceptor;

import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.CacheManager;

/**
 * A {@link CacheResolver} that forces the resolution to a configurable
 * collection of name(s) against a given {@link CacheManager}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class NamedCacheResolver extends AbstractCacheResolver {

	private @Nullable Collection<String> cacheNames;


	public NamedCacheResolver() {
	}

	public NamedCacheResolver(CacheManager cacheManager, String... cacheNames) {
		super(cacheManager);
		this.cacheNames = List.of(cacheNames);
	}


	/**
	 * Set the cache name(s) that this resolver should use.
	 */
	public void setCacheNames(Collection<String> cacheNames) {
		this.cacheNames = cacheNames;
	}

	@Override
	protected @Nullable Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		return this.cacheNames;
	}

}
