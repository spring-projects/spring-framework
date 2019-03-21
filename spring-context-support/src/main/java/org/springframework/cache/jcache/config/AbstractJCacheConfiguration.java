/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cache.annotation.AbstractCachingConfiguration;
import org.springframework.cache.annotation.CachingConfigurer;
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
 * @since 4.1
 * @see JCacheConfigurer
 */
@Configuration
public class AbstractJCacheConfiguration extends AbstractCachingConfiguration {

	protected CacheResolver exceptionCacheResolver;

	@Override
	protected void useCachingConfigurer(CachingConfigurer config) {
		super.useCachingConfigurer(config);
		if (config instanceof JCacheConfigurer) {
			this.exceptionCacheResolver = ((JCacheConfigurer) config).exceptionCacheResolver();
		}
	}

	@Bean(name = "jCacheOperationSource")
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public JCacheOperationSource cacheOperationSource() {
		DefaultJCacheOperationSource source = new DefaultJCacheOperationSource();
		if (this.cacheManager != null) {
			source.setCacheManager(this.cacheManager);
		}
		if (this.keyGenerator != null) {
			source.setKeyGenerator(this.keyGenerator);
		}
		if (this.cacheResolver != null) {
			source.setCacheResolver(this.cacheResolver);
		}
		if (this.exceptionCacheResolver != null) {
			source.setExceptionCacheResolver(this.exceptionCacheResolver);
		}
		return source;
	}

}
