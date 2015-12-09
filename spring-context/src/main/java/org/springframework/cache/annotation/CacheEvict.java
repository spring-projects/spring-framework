/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation indicating that a method (or all methods on a class) triggers a
 * {@link org.springframework.cache.Cache#evict(Object) cache evict} operation.
 *
 * @author Costin Leau
 * @author Stephane Nicoll
 * @since 3.1
 * @see CacheConfig
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CacheEvict {

	/**
	 * Names of the caches to use for the cache eviction operation.
	 * <p>Names may be used to determine the target cache (or caches), matching
	 * the qualifier value or bean name of a specific bean definition.
	 * @see CacheConfig#cacheNames
	 */
	String[] value() default {};

	/**
	 * Spring Expression Language (SpEL) expression for computing the key dynamically.
	 * <p>Default is {@code ""}, meaning all method parameters are considered as a key,
	 * unless a custom {@link #keyGenerator} has been set.
	 * <p>The SpEL expression evaluates again a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #result} for a reference to the result of the method invocation, which
	 * can only be used if {@link #beforeInvocation()} is {@code false}.</li>
	 * <li>{@code #root.method}, {@code #root.target} and {@code #root.caches} for a
	 * reference to the {@link java.lang.reflect.Method method}, target object and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be access via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 */
	String key() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.KeyGenerator}
	 * to use.
	 * <p>Mutually exclusive with the {@link #key} attribute.
	 * @see CacheConfig#keyGenerator
	 */
	String keyGenerator() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
	 * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
	 * is set already.
	 * <p>Mutually exclusive with the {@link #cacheResolver} attribute.
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 * @see CacheConfig#cacheManager
	 */
	String cacheManager() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver}
	 * to use.
	 * @see CacheConfig#cacheResolver
	 */
	String cacheResolver() default "";

	/**
	 * Spring Expression Language (SpEL) expression used for making the cache
	 * eviction operation conditional.
	 * <p>Default is {@code ""}, meaning the cache eviction is always performed.
	 * <p>The SpEL expression evaluates again a dedicated context that provides the
	 * following meta-data:
	 * <ul>
	 * <li>{@code #root.method}, {@code #root.target} and {@code #root.caches} for a
	 * reference to the {@link java.lang.reflect.Method method}, target object and
	 * affected cache(s) respectively.</li>
	 * <li>Shortcuts for the method name ({@code #root.methodName}) and target class
	 * ({@code #root.targetClass}) are also available.
	 * <li>Method arguments can be accessed by index. For instance the second argument
	 * can be access via {@code #root.args[1]}, {@code #p1} or {@code #a1}. Arguments
	 * can also be accessed by name if that information is available.</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * Whether all the entries inside the cache(s) are removed.
	 * <p>By default, only the value under the associated key is removed.
	 * <p>Note that setting this parameter to {@code true} and specifying a
	 * {@link #key} is not allowed.
	 */
	boolean allEntries() default false;

	/**
	 * Whether the eviction should occur before the method is invoked.
	 * <p>Setting this attribute to {@code true}, causes the eviction to
	 * occur irrespective of the method outcome (i.e., whether it threw an
	 * exception or not).
	 * <p>Defaults to {@code false}, meaning that the cache eviction operation
	 * will occur <em>after</em> the advised method is invoked successfully (i.e.,
	 * only if the invocation did not throw an exception).
	 */
	boolean beforeInvocation() default false;

}
