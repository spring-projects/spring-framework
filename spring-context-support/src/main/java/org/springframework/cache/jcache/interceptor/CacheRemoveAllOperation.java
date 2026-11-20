/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.interceptor;

import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheRemoveAll;

import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.util.ExceptionTypeFilter;

/**
 * The {@link JCacheOperation} implementation for a {@link CacheRemoveAll} operation.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CacheRemoveAll
 */
class CacheRemoveAllOperation extends AbstractJCacheOperation<CacheRemoveAll> {

	private final ExceptionTypeFilter exceptionTypeFilter;


	public CacheRemoveAllOperation(CacheMethodDetails<CacheRemoveAll> methodDetails, CacheResolver cacheResolver) {
		super(methodDetails, cacheResolver);
		CacheRemoveAll ann = methodDetails.getCacheAnnotation();
		this.exceptionTypeFilter = createExceptionTypeFilter(ann.evictFor(), ann.noEvictFor());
	}


	@Override
	public ExceptionTypeFilter getExceptionTypeFilter() {
		return this.exceptionTypeFilter;
	}

	/**
	 * Specify if the cache should be cleared before invoking the method. By default, the
	 * cache is cleared after the method invocation.
	 * @see javax.cache.annotation.CacheRemoveAll#afterInvocation()
	 */
	public boolean isEarlyRemove() {
		return !getCacheAnnotation().afterInvocation();
	}

}
