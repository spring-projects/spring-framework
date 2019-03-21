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

import java.lang.reflect.Method;

/**
 * Interface used by {@link JCacheInterceptor}. Implementations know how to source
 * cache operation attributes from standard JSR-107 annotations.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheOperationSource
 */
public interface JCacheOperationSource {

	/**
	 * Return the cache operations for this method, or {@code null}
	 * if the method contains no <em>JSR-107</em> related metadata.
	 *
	 * @param method the method to introspect
	 * @param targetClass the target class (may be {@code null}, in which case
	 * the declaring class of the method must be used)
	 * @return the cache operation for this method, or {@code null} if none found
	 */
	JCacheOperation<?> getCacheOperation(Method method, Class<?> targetClass);

}
