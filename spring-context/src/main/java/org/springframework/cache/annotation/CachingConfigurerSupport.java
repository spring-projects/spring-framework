/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * An implementation of {@link CachingConfigurer} with empty methods allowing
 * subclasses to override only the methods they're interested in.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CachingConfigurer
 * @deprecated as of 6.0 in favor of implementing {@link CachingConfigurer} directly
 */
@Deprecated(since = "6.0")
public class CachingConfigurerSupport implements CachingConfigurer {

	@Override
	public @Nullable CacheManager cacheManager() {
		return null;
	}

	@Override
	public @Nullable CacheResolver cacheResolver() {
		return null;
	}

	@Override
	public @Nullable KeyGenerator keyGenerator() {
		return null;
	}

	@Override
	public @Nullable CacheErrorHandler errorHandler() {
		return null;
	}

}
