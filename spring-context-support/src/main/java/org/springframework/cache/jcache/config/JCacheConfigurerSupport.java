/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.config;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.interceptor.CacheResolver;

/**
 * An extension of {@link CachingConfigurerSupport} that also implements
 * {@link JCacheConfigurer}.
 *
 * <p>Users of JSR-107 annotations may extend from this class rather than
 * implementing from {@link JCacheConfigurer} directly.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see JCacheConfigurer
 * @see CachingConfigurerSupport
 * @deprecated as of 6.0 in favor of implementing {@link JCacheConfigurer} directly
 */
@Deprecated(since = "6.0")
public class JCacheConfigurerSupport extends CachingConfigurerSupport implements JCacheConfigurer {

	@Override
	public @Nullable CacheResolver exceptionCacheResolver() {
		return null;
	}

}
