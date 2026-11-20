/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.config;

/**
 * Configuration constants for internal sharing across subpackages.
 *
 * @author Juergen Hoeller
 * @since 4.1
 */
public abstract class CacheManagementConfigUtils {

	/**
	 * The name of the cache advisor bean.
	 */
	public static final String CACHE_ADVISOR_BEAN_NAME =
			"org.springframework.cache.config.internalCacheAdvisor";

	/**
	 * The name of the cache aspect bean.
	 */
	public static final String CACHE_ASPECT_BEAN_NAME =
			"org.springframework.cache.config.internalCacheAspect";

	/**
	 * The name of the JCache advisor bean.
	 */
	public static final String JCACHE_ADVISOR_BEAN_NAME =
			"org.springframework.cache.config.internalJCacheAdvisor";

	/**
	 * The name of the JCache advisor bean.
	 */
	public static final String JCACHE_ASPECT_BEAN_NAME =
			"org.springframework.cache.config.internalJCacheAspect";

}
