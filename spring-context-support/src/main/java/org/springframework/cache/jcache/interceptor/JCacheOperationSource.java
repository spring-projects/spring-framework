/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * Interface used by {@link JCacheInterceptor}. Implementations know how to source
 * cache operation attributes from standard JSR-107 annotations.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see org.springframework.cache.interceptor.CacheOperationSource
 */
public interface JCacheOperationSource {

	/**
	 * Determine whether the given class is a candidate for cache operations
	 * in the metadata format of this {@code JCacheOperationSource}.
	 * <p>If this method returns {@code false}, the methods on the given class
	 * will not get traversed for {@link #getCacheOperation} introspection.
	 * Returning {@code false} is therefore an optimization for non-affected
	 * classes, whereas {@code true} simply means that the class needs to get
	 * fully introspected for each method on the given class individually.
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no cache operation
	 * metadata at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 6.2
	 * @see #hasCacheOperation
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * Determine whether there is a JSR-107 cache operation for the given method.
	 * @param method the method to introspect
	 * @param targetClass the target class (can be {@code null}, in which case
	 * the declaring class of the method must be used)
	 * @since 6.2
	 * @see #getCacheOperation
	 */
	default boolean hasCacheOperation(Method method, @Nullable Class<?> targetClass) {
		return (getCacheOperation(method, targetClass) != null);
	}

	/**
	 * Return the cache operations for this method, or {@code null}
	 * if the method contains no <em>JSR-107</em> related metadata.
	 * @param method the method to introspect
	 * @param targetClass the target class (can be {@code null}, in which case
	 * the declaring class of the method must be used)
	 * @return the cache operation for this method, or {@code null} if none found
	 */
	@Nullable
	JCacheOperation<?> getCacheOperation(Method method, @Nullable Class<?> targetClass);

}
