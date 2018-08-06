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

package org.springframework.cache.ehcache;

import java.util.Set;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.bootstrap.BootstrapCacheLoader;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;
import net.sf.ehcache.event.CacheEventListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * {@link FactoryBean} that creates a named EhCache {@link net.sf.ehcache.Cache} instance
 * (or a decorator that implements the {@link net.sf.ehcache.Ehcache} interface),
 * representing a cache region within an EhCache {@link net.sf.ehcache.CacheManager}.
 *
 * <p>If the specified named cache is not configured in the cache configuration descriptor,
 * this FactoryBean will construct an instance of a Cache with the provided name and the
 * specified cache properties and add it to the CacheManager for later retrieval. If some
 * or all properties are not set at configuration time, this FactoryBean will use defaults.
 *
 * <p>Note: If the named Cache instance is found, the properties will be ignored and the
 * Cache instance will be retrieved from the CacheManager.
 *
 * <p>Note: As of Spring 5.0, Spring's EhCache support requires EhCache 2.10 or higher.
 *
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 1.1.1
 * @see #setCacheManager
 * @see EhCacheManagerFactoryBean
 * @see net.sf.ehcache.Cache
 */
public class EhCacheFactoryBean extends CacheConfiguration implements FactoryBean<Ehcache>, BeanNameAware, InitializingBean {

	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private CacheManager cacheManager;

	private boolean blocking = false;

	@Nullable
	private CacheEntryFactory cacheEntryFactory;

	@Nullable
	private BootstrapCacheLoader bootstrapCacheLoader;

	@Nullable
	private Set<CacheEventListener> cacheEventListeners;

	private boolean disabled = false;

	@Nullable
	private String beanName;

	@Nullable
	private Ehcache cache;


	public EhCacheFactoryBean() {
		setMaxEntriesLocalHeap(10000);
		setMaxEntriesLocalDisk(10000000);
		setTimeToLiveSeconds(120);
		setTimeToIdleSeconds(120);
	}


	/**
	 * Set a CacheManager from which to retrieve a named Cache instance.
	 * By default, {@code CacheManager.getInstance()} will be called.
	 * <p>Note that in particular for persistent caches, it is advisable to
	 * properly handle the shutdown of the CacheManager: Set up a separate
	 * EhCacheManagerFactoryBean and pass a reference to this bean property.
	 * <p>A separate EhCacheManagerFactoryBean is also necessary for loading
	 * EhCache configuration from a non-default config location.
	 * @see EhCacheManagerFactoryBean
	 * @see net.sf.ehcache.CacheManager#getInstance
	 */
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	/**
	 * Set a name for which to retrieve or create a cache instance.
	 * Default is the bean name of this EhCacheFactoryBean.
	 */
	public void setCacheName(String cacheName) {
		setName(cacheName);
	}

	/**
	 * Set the time to live.
	 * @see #setTimeToLiveSeconds(long)
	 */
	public void setTimeToLive(int timeToLive) {
		setTimeToLiveSeconds(timeToLive);
	}

	/**
	 * Set the time to idle.
	 * @see #setTimeToIdleSeconds(long)
	 */
	public void setTimeToIdle(int timeToIdle) {
		setTimeToIdleSeconds(timeToIdle);
	}

	/**
	 * Set the disk spool buffer size (in MB).
	 * @see #setDiskSpoolBufferSizeMB(int)
	 */
	public void setDiskSpoolBufferSize(int diskSpoolBufferSize) {
		setDiskSpoolBufferSizeMB(diskSpoolBufferSize);
	}

	/**
	 * Set whether to use a blocking cache that lets read attempts block
	 * until the requested element is created.
	 * <p>If you intend to build a self-populating blocking cache,
	 * consider specifying a {@link #setCacheEntryFactory CacheEntryFactory}.
	 * @see net.sf.ehcache.constructs.blocking.BlockingCache
	 * @see #setCacheEntryFactory
	 */
	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	/**
	 * Set an EhCache {@link net.sf.ehcache.constructs.blocking.CacheEntryFactory}
	 * to use for a self-populating cache. If such a factory is specified,
	 * the cache will be decorated with EhCache's
	 * {@link net.sf.ehcache.constructs.blocking.SelfPopulatingCache}.
	 * <p>The specified factory can be of type
	 * {@link net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory},
	 * which will lead to the use of an
	 * {@link net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache}.
	 * <p>Note: Any such self-populating cache is automatically a blocking cache.
	 * @see net.sf.ehcache.constructs.blocking.SelfPopulatingCache
	 * @see net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache
	 * @see net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory
	 */
	public void setCacheEntryFactory(CacheEntryFactory cacheEntryFactory) {
		this.cacheEntryFactory = cacheEntryFactory;
	}

	/**
	 * Set an EhCache {@link net.sf.ehcache.bootstrap.BootstrapCacheLoader}
	 * for this cache, if any.
	 */
	public void setBootstrapCacheLoader(BootstrapCacheLoader bootstrapCacheLoader) {
		this.bootstrapCacheLoader = bootstrapCacheLoader;
	}

	/**
	 * Specify EhCache {@link net.sf.ehcache.event.CacheEventListener cache event listeners}
	 * to registered with this cache.
	 */
	public void setCacheEventListeners(Set<CacheEventListener> cacheEventListeners) {
		this.cacheEventListeners = cacheEventListeners;
	}

	/**
	 * Set whether this cache should be marked as disabled.
	 * @see net.sf.ehcache.Cache#setDisabled
	 */
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	@Override
	public void setBeanName(String name) {
		this.beanName = name;
	}


	@Override
	public void afterPropertiesSet() throws CacheException {
		// If no cache name given, use bean name as cache name.
		String cacheName = getName();
		if (cacheName == null) {
			cacheName = this.beanName;
			if (cacheName != null) {
				setName(cacheName);
			}
		}

		// If no CacheManager given, fetch the default.
		if (this.cacheManager == null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Using default EhCache CacheManager for cache region '" + cacheName + "'");
			}
			this.cacheManager = CacheManager.getInstance();
		}

		synchronized (this.cacheManager) {
			// Fetch cache region: If none with the given name exists, create one on the fly.
			Ehcache rawCache;
			boolean cacheExists = this.cacheManager.cacheExists(cacheName);

			if (cacheExists) {
				if (logger.isDebugEnabled()) {
					logger.debug("Using existing EhCache cache region '" + cacheName + "'");
				}
				rawCache = this.cacheManager.getEhcache(cacheName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Creating new EhCache cache region '" + cacheName + "'");
				}
				rawCache = createCache();
				rawCache.setBootstrapCacheLoader(this.bootstrapCacheLoader);
			}

			if (this.cacheEventListeners != null) {
				for (CacheEventListener listener : this.cacheEventListeners) {
					rawCache.getCacheEventNotificationService().registerListener(listener);
				}
			}

			// Needs to happen after listener registration but before setStatisticsEnabled
			if (!cacheExists) {
				this.cacheManager.addCache(rawCache);
			}

			if (this.disabled) {
				rawCache.setDisabled(true);
			}

			Ehcache decoratedCache = decorateCache(rawCache);
			if (decoratedCache != rawCache) {
				this.cacheManager.replaceCacheWithDecoratedCache(rawCache, decoratedCache);
			}
			this.cache = decoratedCache;
		}
	}

	/**
	 * Create a raw Cache object based on the configuration of this FactoryBean.
	 */
	protected Cache createCache() {
		return new Cache(this);
	}

	/**
	 * Decorate the given Cache, if necessary.
	 * @param cache the raw Cache object, based on the configuration of this FactoryBean
	 * @return the (potentially decorated) cache object to be registered with the CacheManager
	 */
	protected Ehcache decorateCache(Ehcache cache) {
		if (this.cacheEntryFactory != null) {
			if (this.cacheEntryFactory instanceof UpdatingCacheEntryFactory) {
				return new UpdatingSelfPopulatingCache(cache, (UpdatingCacheEntryFactory) this.cacheEntryFactory);
			}
			else {
				return new SelfPopulatingCache(cache, this.cacheEntryFactory);
			}
		}
		if (this.blocking) {
			return new BlockingCache(cache);
		}
		return cache;
	}


	@Override
	@Nullable
	public Ehcache getObject() {
		return this.cache;
	}

	/**
	 * Predict the particular {@code Ehcache} implementation that will be returned from
	 * {@link #getObject()} based on logic in {@link #createCache()} and
	 * {@link #decorateCache(Ehcache)} as orchestrated by {@link #afterPropertiesSet()}.
	 */
	@Override
	public Class<? extends Ehcache> getObjectType() {
		if (this.cache != null) {
			return this.cache.getClass();
		}
		if (this.cacheEntryFactory != null) {
			if (this.cacheEntryFactory instanceof UpdatingCacheEntryFactory) {
				return UpdatingSelfPopulatingCache.class;
			}
			else {
				return SelfPopulatingCache.class;
			}
		}
		if (this.blocking) {
			return BlockingCache.class;
		}
		return Cache.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
