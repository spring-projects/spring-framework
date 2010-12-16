/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.cache.interceptor.CacheDefinition;


/**
 * Strategy interface for parsing known caching annotation types.
 * {@link AnnotationCacheDefinitionSource} delegates to such
 * parsers for supporting specific annotation types such as Spring's own
 * {@link Cacheable} or {@link CacheEvict}.
 *
 * @author Costin Leau
 */
public interface CacheAnnotationParser {

	/**
	 * Parses the cache definition for the given method or class,
	 * based on a known annotation type.
	 * <p>This essentially parses a known cache annotation into Spring's
	 * metadata attribute class. Returns <code>null</code> if the method/class
	 * is not cacheable.
	 * @param ae the annotated method or class
	 * @return CacheOperationDefinition the configured caching operation,
	 * or <code>null</code> if none was found
	 * @see AnnotationCacheDefinitionSource#determineCacheOperationDefinition
	 */
	CacheDefinition parseCacheAnnotation(AnnotatedElement ae);
}
