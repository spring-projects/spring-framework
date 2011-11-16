package org.springframework.cache.annotation;

import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * Interface to be implemented by @{@link org.springframework.context.annotation.Configuration
 * Configuration} classes annotated with @{@link EnableCaching} that wish or need to
 * specify explicitly the {@link CacheManager} and {@link KeyGenerator} beans to be used
 * for annotation-driven cache management.
 *
 * <p>See @{@link EnableCaching} for general examples and context; see
 * {@link #cacheManager()} and {@link #keyGenerator()} for detailed instructions.
 *
 * @author Chris Beams
 * @since 3.1
 * @see EnableCaching
 */
public interface CachingConfigurer {

	/**
	 * Return the cache manager bean to use for annotation-driven cache management.
	 * Implementations must explicitly declare
	 * {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig implements CachingConfigurer {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public CacheManager cacheManager() {
	 *         // configure and return CacheManager instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See @{@link EnableCaching} for more complete examples.
	 */
	CacheManager cacheManager();

	/**
	 * Return the key generator bean to use for annotation-driven cache management.
	 * Implementations must explicitly declare
	 * {@link org.springframework.context.annotation.Bean @Bean}, e.g.
	 * <pre class="code">
	 * &#064;Configuration
	 * &#064;EnableCaching
	 * public class AppConfig implements CachingConfigurer {
	 *     &#064;Bean // important!
	 *     &#064;Override
	 *     public KeyGenerator keyGenerator() {
	 *         // configure and return KeyGenerator instance
	 *     }
	 *     // ...
	 * }
	 * </pre>
	 * See @{@link EnableCaching} for more complete examples.
	 */
	KeyGenerator keyGenerator();

}
