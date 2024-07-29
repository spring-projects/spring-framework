/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.lang.Nullable;

/**
 * Interface to be implemented by @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableCaching} that wish or need to specify
 * explicitly how caches are resolved and how keys are generated for annotation-driven
 * cache management.
 *
 * <p>See @{@link EnableCaching} for general examples and context; see
 * {@link #cacheManager()}, {@link #cacheResolver()}, {@link #keyGenerator()}, and
 * {@link #errorHandler()} for detailed instructions.
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableCaching
 */
public interface CachingConfigurer {

	/**
	 * Return the cache manager bean to use for annotation-driven cache
	 * management. A default {@link CacheResolver} will be initialized
	 * behind the scenes with this cache manager. For more fine-grained
	 * management of the cache resolution, consider setting the
	 * {@link CacheResolver} directly.
	 * <p>Implementations must explicitly declare
	 * {@link org.springframework.context.annotation.Bean @Bean} so that
	 * the cache manager participates in the lifecycle of the context, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * class AppConfig implements CachingConfigurer {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     CacheManager cacheManager() {
	 *         // configure and return CacheManager instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See @{@link EnableCaching} for more complete examples.
	 */
	@Nullable
	default CacheManager cacheManager() {
		return null;
	}

	/**
	 * Return the {@link CacheResolver} bean to use to resolve regular caches for
	 * annotation-driven cache management. This is an alternative and more powerful
	 * option of specifying the {@link CacheManager} to use.
	 * <p>If both a {@link #cacheManager()} and {@code cacheResolver()} are set,
	 * the cache manager is ignored.
	 * <p>Implementations must explicitly declare
	 * {@link org.springframework.context.annotation.Bean @Bean} so that
	 * the cache resolver participates in the lifecycle of the context, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * class AppConfig implements CachingConfigurer {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     CacheResolver cacheResolver() {
	 *         // configure and return CacheResolver instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See {@link EnableCaching} for more complete examples.
	 */
	@Nullable
	default CacheResolver cacheResolver() {
		return null;
	}

	/**
	 * Return the key generator bean to use for annotation-driven cache management.
	 * <p>By default, {@link org.springframework.cache.interceptor.SimpleKeyGenerator}
	 * is used.
	 * See @{@link EnableCaching} for more complete examples.
	 */
	@Nullable
	default KeyGenerator keyGenerator() {
		return null;
	}

	/**
	 * Return the {@link CacheErrorHandler} to use to handle cache-related errors.
	 * <p>By default, {@link org.springframework.cache.interceptor.SimpleCacheErrorHandler}
	 * is used, which throws the exception back at the client.
	 * See @{@link EnableCaching} for more complete examples.
	 */
	@Nullable
	default CacheErrorHandler errorHandler() {
		return null;
	}

}
