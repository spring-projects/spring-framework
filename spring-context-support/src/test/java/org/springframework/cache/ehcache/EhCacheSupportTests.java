/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.cache.ehcache;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Dmitriy Kopylenko
 * @since 27.09.2004
 */
public class EhCacheSupportTests {

	@Test
	public void testBlankCacheManager() {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.setCacheManagerName("myCacheManager");
		assertThat(cacheManagerFb.getObjectType()).isEqualTo(CacheManager.class);
		assertThat(cacheManagerFb.isSingleton()).as("Singleton property").isTrue();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			assertThat(cm.getCacheNames().length == 0).as("Loaded CacheManager with no caches").isTrue();
			Cache myCache1 = cm.getCache("myCache1");
			assertThat(myCache1 == null).as("No myCache1 defined").isTrue();
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	@Test
	public void testCacheManagerConflict() {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		try {
			cacheManagerFb.setCacheManagerName("myCacheManager");
			assertThat(cacheManagerFb.getObjectType()).isEqualTo(CacheManager.class);
			assertThat(cacheManagerFb.isSingleton()).as("Singleton property").isTrue();
			cacheManagerFb.afterPropertiesSet();
			CacheManager cm = cacheManagerFb.getObject();
			assertThat(cm.getCacheNames().length == 0).as("Loaded CacheManager with no caches").isTrue();
			Cache myCache1 = cm.getCache("myCache1");
			assertThat(myCache1 == null).as("No myCache1 defined").isTrue();

			EhCacheManagerFactoryBean cacheManagerFb2 = new EhCacheManagerFactoryBean();
			cacheManagerFb2.setCacheManagerName("myCacheManager");
			assertThatExceptionOfType(CacheException.class).as("because of naming conflict").isThrownBy(
					cacheManagerFb2::afterPropertiesSet);
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	@Test
	public void testAcceptExistingCacheManager() {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.setCacheManagerName("myCacheManager");
		assertThat(cacheManagerFb.getObjectType()).isEqualTo(CacheManager.class);
		assertThat(cacheManagerFb.isSingleton()).as("Singleton property").isTrue();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			assertThat(cm.getCacheNames().length == 0).as("Loaded CacheManager with no caches").isTrue();
			Cache myCache1 = cm.getCache("myCache1");
			assertThat(myCache1 == null).as("No myCache1 defined").isTrue();

			EhCacheManagerFactoryBean cacheManagerFb2 = new EhCacheManagerFactoryBean();
			cacheManagerFb2.setCacheManagerName("myCacheManager");
			cacheManagerFb2.setAcceptExisting(true);
			cacheManagerFb2.afterPropertiesSet();
			CacheManager cm2 = cacheManagerFb2.getObject();
			assertThat(cm2).isSameAs(cm);
			cacheManagerFb2.destroy();
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	public void testCacheManagerFromConfigFile() {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.setConfigLocation(new ClassPathResource("testEhcache.xml", getClass()));
		cacheManagerFb.setCacheManagerName("myCacheManager");
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			assertThat(cm.getCacheNames().length == 1).as("Correct number of caches loaded").isTrue();
			Cache myCache1 = cm.getCache("myCache1");
			assertThat(myCache1.getCacheConfiguration().isEternal()).as("myCache1 is not eternal").isFalse();
			assertThat(myCache1.getCacheConfiguration().getMaxEntriesLocalHeap() == 300).as("myCache1.maxElements == 300").isTrue();
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	@Test
	public void testEhCacheFactoryBeanWithDefaultCacheManager() {
		doTestEhCacheFactoryBean(false);
	}

	@Test
	public void testEhCacheFactoryBeanWithExplicitCacheManager() {
		doTestEhCacheFactoryBean(true);
	}

	private void doTestEhCacheFactoryBean(boolean useCacheManagerFb) {
		Cache cache;
		EhCacheManagerFactoryBean cacheManagerFb = null;
		boolean cacheManagerFbInitialized = false;
		try {
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			Class<? extends Ehcache> objectType = cacheFb.getObjectType();
			assertThat(Ehcache.class.isAssignableFrom(objectType)).isTrue();
			assertThat(cacheFb.isSingleton()).as("Singleton property").isTrue();
			if (useCacheManagerFb) {
				cacheManagerFb = new EhCacheManagerFactoryBean();
				cacheManagerFb.setConfigLocation(new ClassPathResource("testEhcache.xml", getClass()));
				cacheManagerFb.setCacheManagerName("cache");
				cacheManagerFb.afterPropertiesSet();
				cacheManagerFbInitialized = true;
				cacheFb.setCacheManager(cacheManagerFb.getObject());
			}

			cacheFb.setCacheName("myCache1");
			cacheFb.afterPropertiesSet();
			cache = (Cache) cacheFb.getObject();
			Class<? extends Ehcache> objectType2 = cacheFb.getObjectType();
			assertThat(objectType2).isSameAs(objectType);
			CacheConfiguration config = cache.getCacheConfiguration();
			assertThat(cache.getName()).isEqualTo("myCache1");
			if (useCacheManagerFb){
				assertThat(config.getMaxEntriesLocalHeap()).as("myCache1.maxElements").isEqualTo(300);
			}
			else {
				assertThat(config.getMaxEntriesLocalHeap()).as("myCache1.maxElements").isEqualTo(10000);
			}

			// Cache region is not defined. Should create one with default properties.
			cacheFb = new EhCacheFactoryBean();
			if (useCacheManagerFb) {
				cacheFb.setCacheManager(cacheManagerFb.getObject());
			}
			cacheFb.setCacheName("undefinedCache");
			cacheFb.afterPropertiesSet();
			cache = (Cache) cacheFb.getObject();
			config = cache.getCacheConfiguration();
			assertThat(cache.getName()).isEqualTo("undefinedCache");
			assertThat(config.getMaxEntriesLocalHeap() == 10000).as("default maxElements is correct").isTrue();
			assertThat(config.isEternal()).as("default eternal is correct").isFalse();
			assertThat(config.getTimeToLiveSeconds() == 120).as("default timeToLive is correct").isTrue();
			assertThat(config.getTimeToIdleSeconds() == 120).as("default timeToIdle is correct").isTrue();
			assertThat(config.getDiskExpiryThreadIntervalSeconds() == 120).as("default diskExpiryThreadIntervalSeconds is correct").isTrue();

			// overriding the default properties
			cacheFb = new EhCacheFactoryBean();
			if (useCacheManagerFb) {
				cacheFb.setCacheManager(cacheManagerFb.getObject());
			}
			cacheFb.setBeanName("undefinedCache2");
			cacheFb.setMaxEntriesLocalHeap(5);
			cacheFb.setTimeToLive(8);
			cacheFb.setTimeToIdle(7);
			cacheFb.setDiskExpiryThreadIntervalSeconds(10);
			cacheFb.afterPropertiesSet();
			cache = (Cache) cacheFb.getObject();
			config = cache.getCacheConfiguration();

			assertThat(cache.getName()).isEqualTo("undefinedCache2");
			assertThat(config.getMaxEntriesLocalHeap() == 5).as("overridden maxElements is correct").isTrue();
			assertThat(config.getTimeToLiveSeconds() == 8).as("default timeToLive is correct").isTrue();
			assertThat(config.getTimeToIdleSeconds() == 7).as("default timeToIdle is correct").isTrue();
			assertThat(config.getDiskExpiryThreadIntervalSeconds() == 10).as("overridden diskExpiryThreadIntervalSeconds is correct").isTrue();
		}
		finally {
			if (cacheManagerFbInitialized) {
				cacheManagerFb.destroy();
			}
			else {
				CacheManager.getInstance().shutdown();
			}
		}
	}

	@Test
	public void testEhCacheFactoryBeanWithBlockingCache() {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			cacheFb.setCacheManager(cm);
			cacheFb.setCacheName("myCache1");
			cacheFb.setBlocking(true);
			assertThat(BlockingCache.class).isEqualTo(cacheFb.getObjectType());
			cacheFb.afterPropertiesSet();
			Ehcache myCache1 = cm.getEhcache("myCache1");
			boolean condition = myCache1 instanceof BlockingCache;
			assertThat(condition).isTrue();
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	@Test
	public void testEhCacheFactoryBeanWithSelfPopulatingCache() {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			cacheFb.setCacheManager(cm);
			cacheFb.setCacheName("myCache1");
			cacheFb.setCacheEntryFactory(key -> key);
			assertThat(SelfPopulatingCache.class).isEqualTo(cacheFb.getObjectType());
			cacheFb.afterPropertiesSet();
			Ehcache myCache1 = cm.getEhcache("myCache1");
			boolean condition = myCache1 instanceof SelfPopulatingCache;
			assertThat(condition).isTrue();
			assertThat(myCache1.get("myKey1").getObjectValue()).isEqualTo("myKey1");
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	@Test
	public void testEhCacheFactoryBeanWithUpdatingSelfPopulatingCache() {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			cacheFb.setCacheManager(cm);
			cacheFb.setCacheName("myCache1");
			cacheFb.setCacheEntryFactory(new UpdatingCacheEntryFactory() {
				@Override
				public Object createEntry(Object key) {
					return key;
				}
				@Override
				public void updateEntryValue(Object key, Object value) {
				}
			});
			assertThat(UpdatingSelfPopulatingCache.class).isEqualTo(cacheFb.getObjectType());
			cacheFb.afterPropertiesSet();
			Ehcache myCache1 = cm.getEhcache("myCache1");
			boolean condition = myCache1 instanceof UpdatingSelfPopulatingCache;
			assertThat(condition).isTrue();
			assertThat(myCache1.get("myKey1").getObjectValue()).isEqualTo("myKey1");
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

}
