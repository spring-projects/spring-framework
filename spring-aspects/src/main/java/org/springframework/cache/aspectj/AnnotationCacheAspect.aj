/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.cache.aspectj;

import org.springframework.cache.annotation.AnnotationCacheOperationSource;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;

/**
 * Concrete AspectJ cache aspect using Spring's @{@link Cacheable} annotation.
 *
 * <p>When using this aspect, you <i>must</i> annotate the implementation class (and/or
 * methods within that class), <i>not</i> the interface (if any) that the class
 * implements. AspectJ follows Java's rule that annotations on interfaces are <i>not</i>
 * inherited.
 *
 * <p>A {@code @Cacheable} annotation on a class specifies the default caching semantics
 * for the execution of any <b>public</b> operation in the class.
 *
 * <p>A {@code @Cacheable} annotation on a method within the class overrides the default
 * caching semantics given by the class annotation (if present). Any method may be
 * annotated (regardless of visibility). Annotating non-public methods directly is the
 * only way to get caching demarcation for the execution of such operations.
 *
 * @author Costin Leau
 * @since 3.1
 */
public aspect AnnotationCacheAspect extends AbstractCacheAspect {

	public AnnotationCacheAspect() {
		super(new AnnotationCacheOperationSource(false));
	}

	/**
	 * Matches the execution of any public method in a type with the @{@link Cacheable}
	 * annotation, or any subtype of a type with the {@code @Cacheable} annotation.
	 */
	private pointcut executionOfAnyPublicMethodInAtCacheableType() :
		execution(public * ((@Cacheable *)+).*(..)) && within(@Cacheable *);

	/**
	 * Matches the execution of any public method in a type with the @{@link CacheEvict}
	 * annotation, or any subtype of a type with the {@code CacheEvict} annotation.
	 */
	private pointcut executionOfAnyPublicMethodInAtCacheEvictType() :
		execution(public * ((@CacheEvict *)+).*(..)) && within(@CacheEvict *);

	/**
	 * Matches the execution of any public method in a type with the @{@link CachePut}
	 * annotation, or any subtype of a type with the {@code CachePut} annotation.
	 */
	private pointcut executionOfAnyPublicMethodInAtCachePutType() :
		execution(public * ((@CachePut *)+).*(..)) && within(@CachePut *);

	/**
	 * Matches the execution of any public method in a type with the @{@link Caching}
	 * annotation, or any subtype of a type with the {@code Caching} annotation.
	 */
	private pointcut executionOfAnyPublicMethodInAtCachingType() :
		execution(public * ((@Caching *)+).*(..)) && within(@Caching *);

	/**
	 * Matches the execution of any method with the @{@link Cacheable} annotation.
	 */
	private pointcut executionOfCacheableMethod() :
		execution(@Cacheable * *(..));

	/**
	 * Matches the execution of any method with the @{@link CacheEvict} annotation.
	 */
	private pointcut executionOfCacheEvictMethod() :
		execution(@CacheEvict * *(..));

	/**
	 * Matches the execution of any method with the @{@link CachePut} annotation.
	 */
	private pointcut executionOfCachePutMethod() :
		execution(@CachePut * *(..));

	/**
	 * Matches the execution of any method with the @{@link Caching} annotation.
	 */
	private pointcut executionOfCachingMethod() :
		execution(@Caching * *(..));

	/**
	 * Definition of pointcut from super aspect - matched join points will have Spring
	 * cache management applied.
	 */
	protected pointcut cacheMethodExecution(Object cachedObject) :
		(executionOfAnyPublicMethodInAtCacheableType()
				|| executionOfAnyPublicMethodInAtCacheEvictType()
				|| executionOfAnyPublicMethodInAtCachePutType()
				|| executionOfAnyPublicMethodInAtCachingType()
				|| executionOfCacheableMethod()
				|| executionOfCacheEvictMethod()
				|| executionOfCachePutMethod()
				|| executionOfCachingMethod())
			&& this(cachedObject);
}