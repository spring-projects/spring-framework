/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.AbstractCacheResolver;
import org.springframework.cache.interceptor.BasicOperation;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * A simple {@link CacheResolver} that resolves the exception cache
 * based on a configurable {@link CacheManager} and the name of the
 * cache: {@link CacheResultOperation#getExceptionCacheName()}.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CacheResultOperation#getExceptionCacheName()
 */
public class SimpleExceptionCacheResolver extends AbstractCacheResolver {

	public SimpleExceptionCacheResolver(CacheManager cacheManager) {
		super(cacheManager);
	}

	@Override
	protected Collection<String> getCacheNames(CacheOperationInvocationContext<?> context) {
		BasicOperation operation = context.getOperation();
		if (!(operation instanceof CacheResultOperation)) {
			throw new IllegalStateException("Could not extract exception cache name from " + operation);
		}
		CacheResultOperation cacheResultOperation = (CacheResultOperation) operation;
		String exceptionCacheName = cacheResultOperation.getExceptionCacheName();
		if (exceptionCacheName != null) {
			return Collections.singleton(exceptionCacheName);
		}
		return null;
	}

}
