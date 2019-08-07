/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
	 * Specify if the cache entry should be remove before invoking the method. By default, the
	 * cache entry is removed after the method invocation.
	 * @see javax.cache.annotation.CacheRemove#afterInvocation()
	 */
	public boolean isEarlyRemove() {
		return !getCacheAnnotation().afterInvocation();
	}

}
