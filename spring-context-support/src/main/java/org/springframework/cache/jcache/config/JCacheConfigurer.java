/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
