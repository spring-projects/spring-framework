/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.cache.Cache;

/**
 * Annotation indicating that a method (or all methods on a class) trigger(s)
 * a {@link Cache#put(Object, Object)} operation. As opposed to {@link Cacheable} annotation,
 * this annotation does not cause the target method to be skipped - rather it
 * always causes the method to be invoked and its result to be placed into the cache.
 *
 * @author Costin Leau
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 3.1
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CachePut {

	/**
	 * Name of the caches in which the update takes place.
	 * <p>May be used to determine the target cache (or caches), matching the
	 * qualifier value (or the bean name(s)) of (a) specific bean definition.
	 */
	String[] value();

	/**
	 * Spring Expression Language (SpEL) attribute for computing the key dynamically.
	 * <p>Default is "", meaning all method parameters are considered as a key, unless
	 * a custom {@link #keyGenerator()} has been set.
	 */
	String key() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.KeyGenerator} to use.
	 * <p>Mutually exclusive with the {@link #key()} attribute.
	 */
	String keyGenerator() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.CacheManager} to use.
	 */
	String cacheManager() default "";

	/**
	 * Spring Expression Language (SpEL) attribute used for conditioning the cache update.
	 * <p>Default is "", meaning the method result is always cached.
	 */
	String condition() default "";

	/**
	 * Spring Expression Language (SpEL) attribute used to veto the cache update.
	 * <p>Unlike {@link #condition()}, this expression is evaluated after the method
	 * has been called and can therefore refer to the {@code result}. Default is "",
	 * meaning that caching is never vetoed.
	 * @since 3.2
	 */
	String unless() default "";
}
