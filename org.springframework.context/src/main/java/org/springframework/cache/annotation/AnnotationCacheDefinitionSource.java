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

import java.io.Serializable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.cache.interceptor.AbstractFallbackCacheDefinitionSource;
import org.springframework.cache.interceptor.CacheDefinition;
import org.springframework.util.Assert;

/**
 * 
 * Implementation of the
 * {@link org.springframework.cache.interceptor.CacheDefinitionSource}
 * interface for working with caching metadata in JDK 1.5+ annotation format.
 *
 * <p>This class reads Spring's JDK 1.5+ {@link Cacheable} and {@link CacheEvict} 
 * annotations and
 * exposes corresponding caching operation definition to Spring's cache infrastructure.
 * This class may also serve as base class for a custom CacheDefinitionSource.
 *
 * @author Costin Leau
 */
@SuppressWarnings("serial")
public class AnnotationCacheDefinitionSource extends AbstractFallbackCacheDefinitionSource implements
		Serializable {

	private final boolean publicMethodsOnly;

	private final Set<CacheAnnotationParser> annotationParsers;

	/**
	 * Create a default AnnotationCacheOperationDefinitionSource, supporting
	 * public methods that carry the <code>Cacheable</code> and <code>CacheInvalidate</code>
	 * annotations.
	 */
	public AnnotationCacheDefinitionSource() {
		this(true);
	}

	/**
	 * Create a custom AnnotationCacheOperationDefinitionSource, supporting
	 * public methods that carry the <code>Cacheable</code> and 
	 * <code>CacheInvalidate</code> annotations.
	 *  
	 * @param publicMethodsOnly whether to support only annotated public methods
	 * typically for use with proxy-based AOP), or protected/private methods as well
	 * (typically used with AspectJ class weaving)
	 */
	public AnnotationCacheDefinitionSource(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
		this.annotationParsers = new LinkedHashSet<CacheAnnotationParser>(1);
		this.annotationParsers.add(new SpringCachingAnnotationParser());
	}

	/**
	 * Create a custom AnnotationCacheOperationDefinitionSource.
	 * @param annotationParsers the CacheAnnotationParser to use
	 */
	public AnnotationCacheDefinitionSource(CacheAnnotationParser... annotationParsers) {
		this.publicMethodsOnly = true;
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		Set<CacheAnnotationParser> parsers = new LinkedHashSet<CacheAnnotationParser>(annotationParsers.length);
		Collections.addAll(parsers, annotationParsers);
		this.annotationParsers = parsers;
	}

	@Override
	protected CacheDefinition findCacheDefinition(Class<?> clazz) {
		return determineCacheDefinition(clazz);
	}

	@Override
	protected CacheDefinition findCacheOperation(Method method) {
		return determineCacheDefinition(method);
	}

	/**
	 * Determine the cache operation definition for the given method or class.
	 * <p>This implementation delegates to configured
	 * {@link CacheAnnotationParser CacheAnnotationParsers}
	 * for parsing known annotations into Spring's metadata attribute class.
	 * Returns <code>null</code> if it's not cacheable.
	 * <p>Can be overridden to support custom annotations that carry caching metadata.
	 * @param ae the annotated method or class
	 * @return CacheOperationDefinition the configured caching operation,
	 * or <code>null</code> if none was found
	 */
	protected CacheDefinition determineCacheDefinition(AnnotatedElement ae) {
		for (CacheAnnotationParser annotationParser : this.annotationParsers) {
			CacheDefinition attr = annotationParser.parseCacheAnnotation(ae);
			if (attr != null) {
				return attr;
			}
		}
		return null;
	}

	/**
	 * By default, only public methods can be made cacheable.
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}
}