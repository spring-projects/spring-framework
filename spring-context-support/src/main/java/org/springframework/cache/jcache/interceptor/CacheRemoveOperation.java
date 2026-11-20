/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemove;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.util.ExceptionTypeFilter;

/**
 * The {@link JCacheOperation} implementation for a {@link CacheRemove} operation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CacheRemove
 */
class CacheRemoveOperation extends AbstractJCacheKeyOperation<CacheRemove> {

	private final ExceptionTypeFilter exceptionTypeFilter;


	public CacheRemoveOperation(
			CacheMethodDetails<CacheRemove> methodDetails, CacheResolver cacheResolver, KeyGenerator keyGenerator) {

		super(methodDetails, cacheResolver, keyGenerator);
		CacheRemove ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * Specify if the cache entry should be removed before invoking the method.
	 * <p>By default, the cache entry is removed after the method invocation.
	 * @see javax.cache.annotation.CacheRemove#afterInvocation()
	 */
	public boolean isEarlyRemove() {
		return !getCacheAnnotation().afterInvocation();
	}

}
