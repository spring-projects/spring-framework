/*
 * Copyright 2002-present the original author or authors.
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

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.interceptor.AbstractFallbackCacheOperationSource;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.util.Assert;

/**
 * Implementation of the {@link org.springframework.cache.interceptor.CacheOperationSource
 * CacheOperationSource} interface for working with caching metadata in annotation format.
 *
 * <p>This class reads Spring's {@link Cacheable}, {@link CachePut} and {@link CacheEvict}
 * annotations and exposes corresponding caching operation definition to Spring's cache
 * infrastructure. This class may also serve as base class for a custom
 * {@code CacheOperationSource}.
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 3.1
 */
@SuppressWarnings("serial")
public class AnnotationCacheOperationSource extends AbstractFallbackCacheOperationSource implements Serializable {

	private final Set<CacheAnnotationParser> annotationParsers;

	private boolean publicMethodsOnly = true;


	/**
	 * Create a default AnnotationCacheOperationSource, supporting public methods
	 * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
	 */
	public AnnotationCacheOperationSource() {
		this.annotationParsers = Collections.singleton(new SpringCacheAnnotationParser());
	}

	/**
	 * Create a default {@code AnnotationCacheOperationSource}, supporting public methods
	 * that carry the {@code Cacheable} and {@code CacheEvict} annotations.
	 * @param publicMethodsOnly whether to support only annotated public methods
	 * typically for use with proxy-based AOP), or protected/private methods as well
	 * (typically used with AspectJ class weaving)
	 * @see #setPublicMethodsOnly
	 */
	public AnnotationCacheOperationSource(boolean publicMethodsOnly) {
		this();
		this.publicMethodsOnly = publicMethodsOnly;
	}

	/**
	 * Create a custom AnnotationCacheOperationSource.
	 * @param annotationParser the CacheAnnotationParser to use
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser annotationParser) {
		Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
		this.annotationParsers = Collections.singleton(annotationParser);
	}

	/**
	 * Create a custom AnnotationCacheOperationSource.
	 * @param annotationParsers the CacheAnnotationParser to use
	 */
	public AnnotationCacheOperationSource(CacheAnnotationParser... annotationParsers) {
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		this.annotationParsers = Set.of(annotationParsers);
	}

	/**
	 * Create a custom AnnotationCacheOperationSource.
	 * @param annotationParsers the CacheAnnotationParser to use
	 */
	public AnnotationCacheOperationSource(Set<CacheAnnotationParser> annotationParsers) {
		Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
		this.annotationParsers = annotationParsers;
	}


	/**
	 * Set whether cacheable methods are expected to be public.
	 * <p>The default is {@code true}.
	 * @since 6.2
	 */
	public void setPublicMethodsOnly(boolean publicMethodsOnly) {
		this.publicMethodsOnly = publicMethodsOnly;
	}


	@Override
	public boolean isCandidateClass(Class<?> targetClass) {
		for (CacheAnnotationParser parser : this.annotationParsers) {
			if (parser.isCandidateClass(targetClass)) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected @Nullable Collection<CacheOperation> findCacheOperations(Class<?> clazz) {
		return determineCacheOperations(parser -> parser.parseCacheAnnotations(clazz));
	}

	@Override
	protected @Nullable Collection<CacheOperation> findCacheOperations(Method method) {
		return determineCacheOperations(parser -> parser.parseCacheAnnotations(method));
	}

	/**
	 * Determine the cache operation(s) for the given {@link CacheOperationProvider}.
	 * <p>This implementation delegates to configured
	 * {@link CacheAnnotationParser CacheAnnotationParsers}
	 * for parsing known annotations into Spring's metadata attribute class.
	 * <p>Can be overridden to support custom annotations that carry caching metadata.
	 * @param provider the cache operation provider to use
	 * @return the configured caching operations, or {@code null} if none found
	 */
	protected @Nullable Collection<CacheOperation> determineCacheOperations(CacheOperationProvider provider) {
		Collection<CacheOperation> ops = null;
		for (CacheAnnotationParser parser : this.annotationParsers) {
			Collection<CacheOperation> annOps = provider.getCacheOperations(parser);
			if (annOps != null) {
				if (ops == null) {
					ops = annOps;
				}
				else {
					Collection<CacheOperation> combined = new ArrayList<>(ops.size() + annOps.size());
					combined.addAll(ops);
					combined.addAll(annOps);
					ops = combined;
				}
			}
		}
		return ops;
	}

	/**
	 * By default, only public methods can be made cacheable.
	 * @see #setPublicMethodsOnly
	 */
	@Override
	protected boolean allowPublicMethodsOnly() {
		return this.publicMethodsOnly;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof AnnotationCacheOperationSource otherCos &&
				this.annotationParsers.equals(otherCos.annotationParsers) &&
				this.publicMethodsOnly == otherCos.publicMethodsOnly));
	}

	@Override
	public int hashCode() {
		return this.annotationParsers.hashCode();
	}


	/**
	 * Callback interface providing {@link CacheOperation} instance(s) based on
	 * a given {@link CacheAnnotationParser}.
	 */
	@FunctionalInterface
	protected interface CacheOperationProvider {

		/**
		 * Return the {@link CacheOperation} instance(s) provided by the specified parser.
		 * @param parser the parser to use
		 * @return the cache operations, or {@code null} if none found
		 */
		@Nullable Collection<CacheOperation> getCacheOperations(CacheAnnotationParser parser);
	}

}
