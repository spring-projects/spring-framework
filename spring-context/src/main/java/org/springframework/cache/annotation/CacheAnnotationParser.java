/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cache.annotation;

import java.lang.reflect.Method;
import java.util.Collection;

import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

/**
 * Strategy interface for parsing known caching annotation types.
 * {@link AnnotationCacheOperationSource} delegates to such parsers
 * for supporting specific annotation types such as Spring's own
 * {@link Cacheable}, {@link CachePut} and{@link CacheEvict}.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 3.1
 * @see AnnotationCacheOperationSource
 * @see SpringCacheAnnotationParser
 */
public interface CacheAnnotationParser {

	/**
	 * Determine whether the given class is a candidate for cache operations
	 * in the annotation format of this {@code CacheAnnotationParser}.
	 * <p>If this method returns {@code false}, the methods on the given class
	 * will not get traversed for {@code #parseCacheAnnotations} introspection.
	 * Returning {@code false} is therefore an optimization for non-affected
	 * classes, whereas {@code true} simply means that the class needs to get
	 * fully introspected for each method on the given class individually.
	 * @param targetClass the class to introspect
	 * @return {@code false} if the class is known to have no cache operation
	 * annotations at class or method level; {@code true} otherwise. The default
	 * implementation returns {@code true}, leading to regular introspection.
	 * @since 5.2
	 */
	default boolean isCandidateClass(Class<?> targetClass) {
		return true;
	}

	/**
	 * Parse the cache definition for the given class,
	 * based on an annotation type understood by this parser.
	 * <p>This essentially parses a known cache annotation into Spring's metadata
	 * attribute class. Returns {@code null} if the class is not cacheable.
	 * @param type the annotated class
	 * @return the configured caching operation, or {@code null} if none found
	 * @see AnnotationCacheOperationSource#findCacheOperations(Class)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Class<?> type);

	/**
	 * Parse the cache definition for the given method,
	 * based on an annotation type understood by this parser.
	 * <p>This essentially parses a known cache annotation into Spring's metadata
	 * attribute class. Returns {@code null} if the method is not cacheable.
	 * @param method the annotated method
	 * @return the configured caching operation, or {@code null} if none found
	 * @see AnnotationCacheOperationSource#findCacheOperations(Method)
	 */
	@Nullable
	Collection<CacheOperation> parseCacheAnnotations(Method method);

}
