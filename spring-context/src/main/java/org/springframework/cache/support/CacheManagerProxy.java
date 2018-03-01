/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.cache.support;

import java.util.Collection;
import java.util.function.UnaryOperator;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Proxy for a target {@link CacheManager} that decorates caches using the configured
 * {@link #setCacheDecoratorFactory cache decorator factory}.
 *
 * @author Juergen Hoeller
 * @see #setTargetCacheManager
 * @see #setCacheDecoratorFactory
 */
public class CacheManagerProxy implements CacheManager, InitializingBean {

	@Nullable
	private CacheManager targetCacheManager;

	@Nullable
	private UnaryOperator<Cache> cacheDecoratorFactory;

	/**
	 * Create a new CacheManagerProxy, setting the target
	 * {@link #setTargetCacheManager cache manager} and
	 * {@link #setCacheDecoratorFactory cache decorator factory}
	 * through the bean property setters.
	 */
	public CacheManagerProxy() {
	}

	/**
	 * Create a new CacheManagerProxy for the given target CacheManager.
	 * @param targetCacheManager the target CacheManager to proxy
	 * @param cacheDecoratorFactory factory to use to decorate caches
	 */
	public CacheManagerProxy(CacheManager targetCacheManager, UnaryOperator<Cache> cacheDecoratorFactory) {
		Assert.notNull(targetCacheManager, "Target CacheManager must not be null");
		Assert.notNull(cacheDecoratorFactory, "cacheDecoratorFactory must not be null");
		this.targetCacheManager = targetCacheManager;
		this.cacheDecoratorFactory = cacheDecoratorFactory;
	}


	/**
	 * Set the target CacheManager to proxy.
	 */
	public void setTargetCacheManager(CacheManager targetCacheManager) {
		this.targetCacheManager = targetCacheManager;
	}

	/**
	 * Set the factory to use to decorate caches.
	 */
	public void setCacheDecoratorFactory(UnaryOperator<Cache> cacheDecoratorFactory) {
		this.cacheDecoratorFactory = cacheDecoratorFactory;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.targetCacheManager == null) {
			throw new IllegalArgumentException("Property 'targetCacheManager' is required");
		}
		if (this.cacheDecoratorFactory == null) {
			throw new IllegalArgumentException("Property 'cacheDecoratorFactory' is required");
		}
	}

	@Override
	@Nullable
	public Cache getCache(String name) {
		Assert.state(this.targetCacheManager != null, "No targetCacheManager set");
		Assert.state(this.cacheDecoratorFactory != null, "No cacheDecoratorFactory set");
		Cache targetCache = this.targetCacheManager.getCache(name);
		return (targetCache != null ? this.cacheDecoratorFactory.apply(targetCache) : null);
	}

	@Override
	public Collection<String> getCacheNames() {
		Assert.state(this.targetCacheManager != null, "No targetCacheManager set");
		return this.targetCacheManager.getCacheNames();
	}
}
