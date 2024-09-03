/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @CacheConfig} provides a mechanism for sharing common cache-related
 * settings at the class level.
 *
 * <p>When this annotation is present on a given class, it provides a set
 * of default settings for any cache operation defined in that class.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 * @see Cacheable
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

	/**
	 * Names of the default caches to consider for caching operations defined
	 * in the annotated class.
	 * <p>If none is set at the operation level, these are used instead of the default.
	 * <p>Names may be used to determine the target cache(s), to be resolved via the
	 * configured {@link #cacheResolver()} which typically delegates to
	 * {@link org.springframework.cache.CacheManager#getCache}.
	 * For further details see {@link Cacheable#cacheNames()}.
	 */
	String[] cacheNames() default {};

	/**
	 * The bean name of the default {@link org.springframework.cache.interceptor.KeyGenerator} to
	 * use for the class.
	 * <p>If none is set at the operation level, this one is used instead of the default.
	 * <p>The key generator is mutually exclusive with the use of a custom key. When such key is
	 * defined for the operation, the value of this key generator is ignored.
	 */
	String keyGenerator() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.CacheManager} to use to
	 * create a default {@link org.springframework.cache.interceptor.CacheResolver} if none
	 * is set already.
	 * <p>If no resolver and no cache manager are set at the operation level, and no cache
	 * resolver is set via {@link #cacheResolver}, this one is used instead of the default.
	 * @see org.springframework.cache.interceptor.SimpleCacheResolver
	 */
	String cacheManager() default "";

	/**
	 * The bean name of the custom {@link org.springframework.cache.interceptor.CacheResolver} to use.
	 * <p>If no resolver and no cache manager are set at the operation level, this one is used
	 * instead of the default.
	 */
	String cacheResolver() default "";

}
