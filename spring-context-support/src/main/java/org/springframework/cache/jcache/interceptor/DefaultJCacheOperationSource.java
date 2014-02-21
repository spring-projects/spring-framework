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

package org.springframework.cache.jcache.interceptor;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

/**
 * The default {@link JCacheOperationSource} implementation delegating
 * default operations to configurable services with sensible defaults
 * when not present.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class DefaultJCacheOperationSource extends AnnotationCacheOperationSource
		implements InitializingBean, ApplicationContextAware {

	private CacheManager cacheManager;

	private KeyGenerator keyGenerator;

	private CacheResolver cacheResolver;

	private CacheResolver exceptionCacheResolver;

	private ApplicationContext applicationContext;

	@Override
	public void afterPropertiesSet() {
		Assert.state((cacheResolver != null && exceptionCacheResolver != null)
				|| cacheManager != null, "'cacheManager' is required if cache resolvers are not set.");
		Assert.state(this.applicationContext != null, "The application context was not injected as it should.");

		if (keyGenerator == null) {
			keyGenerator = new KeyGeneratorAdapter(this, new SimpleCacheKeyGenerator());
		}
		if (cacheResolver == null) {
			cacheResolver = new SimpleCacheResolver(cacheManager);
		}
		if (exceptionCacheResolver == null) {
			exceptionCacheResolver = new SimpleExceptionCacheResolver(cacheManager);
		}
	}

	/**
	 * Set the default {@link CacheManager} to use to lookup cache by name. Only mandatory
	 * if the {@linkplain CacheResolver cache resolvers} have not been set.
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * Set the default {@link KeyGenerator}. If none is set, a default JSR-107 compliant
	 * key generator is used.
	 */
	public void setKeyGenerator(KeyGenerator keyGenerator) {
		this.keyGenerator = keyGenerator;
	}

	/**
	 * Set the {@link CacheResolver} to resolve regular caches. If none is set, a default
	 * implementation using the specified cache manager will be used.
	 */
	public void setCacheResolver(CacheResolver cacheResolver) {
		this.cacheResolver = cacheResolver;
	}

	/**
	 * Set the {@link CacheResolver} to resolve exception caches. If none is set, a default
	 * implementation using the specified cache manager will be used.
	 */
	public void setExceptionCacheResolver(CacheResolver exceptionCacheResolver) {
		this.exceptionCacheResolver = exceptionCacheResolver;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Override
	protected <T> T getBean(Class<T> type) {
		Map<String, T> map = BeanFactoryUtils.beansOfTypeIncludingAncestors(applicationContext, type);
		if (map.size() == 1) {
			return map.values().iterator().next();
		}
		else {
			return BeanUtils.instantiateClass(type);
		}
	}

	@Override
	public CacheResolver getDefaultCacheResolver() {
		return cacheResolver;
	}

	@Override
	public CacheResolver getDefaultExceptionCacheResolver() {
		return exceptionCacheResolver;
	}

	@Override
	public KeyGenerator getDefaultKeyGenerator() {
		return keyGenerator;
	}

}
