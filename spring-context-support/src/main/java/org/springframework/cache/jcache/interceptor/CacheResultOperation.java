/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResult;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;
import org.springframework.util.StringUtils;

/**
 * The {@link JCacheOperation} implementation for a {@link CacheResult} operation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CacheResult
 */
class CacheResultOperation extends AbstractJCacheKeyOperation<CacheResult> {

	private final ExceptionTypeFilter exceptionTypeFilter;

	private final @Nullable CacheResolver exceptionCacheResolver;

	private final @Nullable String exceptionCacheName;


	public CacheResultOperation(CacheMethodDetails<CacheResult> methodDetails, CacheResolver cacheResolver,
			KeyGenerator keyGenerator, @Nullable CacheResolver exceptionCacheResolver) {

		super(methodDetails, cacheResolver, keyGenerator);

		CacheResult ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.cachedExceptions(), ann.nonCachedExceptions());
		this.exceptionCacheResolver = exceptionCacheResolver;
		this.exceptionCacheName = (StringUtils.hasText(ann.exceptionCacheName()) ? ann.exceptionCacheName() : null);
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * Specify if the method should always be invoked regardless of a cache hit.
	 * By default, the method is only invoked in case of a cache miss.
	 * @see javax.cache.annotation.CacheResult#skipGet()
	 */
	public boolean isAlwaysInvoked() {
		return getCacheAnnotation().skipGet();
	}

	/**
	 * Return the {@link CacheResolver} instance to use to resolve the cache to
	 * use for matching exceptions thrown by this operation.
	 */
	public @Nullable CacheResolver getExceptionCacheResolver() {
		return this.exceptionCacheResolver;
	}

	/**
	 * Return the name of the cache to cache exceptions, or {@code null} if
	 * caching exceptions should be disabled.
	 * @see javax.cache.annotation.CacheResult#exceptionCacheName()
	 */
	public @Nullable String getExceptionCacheName() {
		return this.exceptionCacheName;
	}

}
