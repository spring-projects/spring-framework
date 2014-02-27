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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Provide a way to share common cache-related settings at class-level.
 * <p>When this annotation is present on a given class, it provides a set
 * of default settings for any cache operation defined on that class.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheConfig {

	/**
	 * Name of the default caches to consider for a caching operation defined in the class.
	 * <p>If none is set at the operation level, these ones are used instead of the default.
	 * <p>May be used to determine the target cache (or caches), matching the
	 * qualifier value (or the bean name(s)) of (a) specific bean definition.
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
	 * resolver is set on this instance, this one is used instead of the default.
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


