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

package org.springframework.cache.jcache.config;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * Extension of {@link CachingConfigurer} for the JSR-107 implementation.
 *
 * <p>To be implemented by classes annotated with
 * {@link org.springframework.cache.annotation.EnableCaching} that wish
 * or need to specify explicitly how exception caches are resolved for
 * annotation-driven cache management.
 *
 * <p>See {@link org.springframework.cache.annotation.EnableCaching} for
 * general examples and context; see {@link #exceptionCacheResolver()} for
 * detailed instructions.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see CachingConfigurer
 * @see org.springframework.cache.annotation.EnableCaching
 */
public interface JCacheConfigurer extends CachingConfigurer {

	/**
	 * Return the {@link CacheResolver} bean to use to resolve exception caches for
	 * annotation-driven cache management. Implementations must explicitly declare
	 * {@link org.springframework.context.annotation.Bean @Bean}, for example,
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig implements JCacheConfigurer {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheResolver exceptionCacheResolver() {
	 *         // configure and return CacheResolver instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See {@link org.springframework.cache.annotation.EnableCaching} for more complete examples.
	 */
	default @Nullable CacheResolver exceptionCacheResolver() {
		return null;
	}

}
