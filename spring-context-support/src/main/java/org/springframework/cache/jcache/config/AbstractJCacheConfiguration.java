/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.jcache.config;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.jcache.interceptor.DefaultJCacheOperationSource;
import org.springframework.cache.jcache.interceptor.JCacheOperationSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Role;

/**
 * Abstract JSR-107 specific {@code @Configuration} class providing common
 * structure for enabling JSR-107 annotation-driven cache management capability.
 *
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 * @see JCacheConfigurer
 */
@Configuration(proxyBeanMethods = false)
public abstract class AbstractJCacheConfiguration extends AbstractCachingConfiguration {

	protected @Nullable Supplier<@Nullable CacheResolver> exceptionCacheResolver;


	@Override
	protected void useCachingConfigurer(CachingConfigurerSupplier cachingConfigurerSupplier) {
		super.useCachingConfigurer(cachingConfigurerSupplier);
		this.exceptionCacheResolver = cachingConfigurerSupplier.adapt(config -> {
			if (config instanceof JCacheConfigurer jcacheConfigurer) {
				return jcacheConfigurer.exceptionCacheResolver();
			}
			return null;
		});
	}

	@Bean(name = "jCacheOperationSource")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JCacheOperationSource cacheOperationSource() {
		return new DefaultJCacheOperationSource(
				this.cacheManager, this.cacheResolver, this.exceptionCacheResolver, this.keyGenerator);
	}

}
