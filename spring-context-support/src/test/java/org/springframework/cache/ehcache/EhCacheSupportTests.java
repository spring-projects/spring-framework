/*
 * Copyright 2002-2012 the original author or authors.
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

import junit.framework.TestCase;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.constructs.blocking.BlockingCache;
import net.sf.ehcache.constructs.blocking.CacheEntryFactory;
import net.sf.ehcache.constructs.blocking.SelfPopulatingCache;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;

import org.springframework.core.io.ClassPathResource;

/**
 * @author Dmitriy Kopylenko
 * @author Juergen Hoeller
 * @since 27.09.2004
 */
public class EhCacheSupportTests extends TestCase {

	public void testLoadingBlankCacheManager() throws Exception {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		assertEquals(CacheManager.class, cacheManagerFb.getObjectType());
		assertTrue("Singleton property", cacheManagerFb.isSingleton());
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			assertTrue("Loaded CacheManager with no caches", cm.getCacheNames().length == 0);
			Cache myCache1 = cm.getCache("myCache1");
			assertTrue("No myCache1 defined", myCache1 == null);
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	public void testLoadingCacheManagerFromConfigFile() throws Exception {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.setConfigLocation(new ClassPathResource("testEhcache.xml", getClass()));
		cacheManagerFb.setCacheManagerName("myCacheManager");
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			assertTrue("Correct number of caches loaded", cm.getCacheNames().length == 1);
			Cache myCache1 = cm.getCache("myCache1");
			assertFalse("myCache1 is not eternal", myCache1.getCacheConfiguration().isEternal());
			assertTrue("myCache1.maxElements == 300", myCache1.getCacheConfiguration().getMaxElementsInMemory() == 300);
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	public void testEhCacheFactoryBeanWithDefaultCacheManager() throws Exception {
		doTestEhCacheFactoryBean(false);
	}

	public void testEhCacheFactoryBeanWithExplicitCacheManager() throws Exception {
		doTestEhCacheFactoryBean(true);
	}

	private void doTestEhCacheFactoryBean(boolean useCacheManagerFb) throws Exception {
		Cache cache = null;
		EhCacheManagerFactoryBean cacheManagerFb = null;
		try {
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			Class<? extends Ehcache> objectType = cacheFb.getObjectType();
			assertTrue(Ehcache.class.isAssignableFrom(objectType));
			assertTrue("Singleton property", cacheFb.isSingleton());
			if (useCacheManagerFb) {
				cacheManagerFb = new EhCacheManagerFactoryBean();
				cacheManagerFb.setConfigLocation(new ClassPathResource("testEhcache.xml", getClass()));
				cacheManagerFb.afterPropertiesSet();
				cacheFb.setCacheManager(cacheManagerFb.getObject());
			}

			cacheFb.setCacheName("myCache1");
			cacheFb.afterPropertiesSet();
			cache = (Cache) cacheFb.getObject();
			Class<? extends Ehcache> objectType2 = cacheFb.getObjectType();
			assertSame(objectType, objectType2);
			CacheConfiguration config = cache.getCacheConfiguration();
			assertEquals("myCache1", cache.getName());
			if (useCacheManagerFb){
				assertEquals("myCache1.maxElements", 300, config.getMaxElementsInMemory());
			}
			else {
				assertEquals("myCache1.maxElements", 10000, config.getMaxElementsInMemory());
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
			assertEquals("undefinedCache", cache.getName());
			assertTrue("default maxElements is correct", config.getMaxElementsInMemory() == 10000);
			assertTrue("default overflowToDisk is correct", config.isOverflowToDisk());
			assertFalse("default eternal is correct", config.isEternal());
			assertTrue("default timeToLive is correct", config.getTimeToLiveSeconds() == 120);
			assertTrue("default timeToIdle is correct", config.getTimeToIdleSeconds() == 120);
			assertTrue("default diskPersistent is correct", !config.isDiskPersistent());
			assertTrue("default diskExpiryThreadIntervalSeconds is correct", config.getDiskExpiryThreadIntervalSeconds() == 120);

			// overriding the default properties
			cacheFb = new EhCacheFactoryBean();
			if (useCacheManagerFb) {
				cacheFb.setCacheManager(cacheManagerFb.getObject());
			}
			cacheFb.setBeanName("undefinedCache2");
			cacheFb.setMaxElementsInMemory(5);
			cacheFb.setOverflowToDisk(false);
			cacheFb.setEternal(true);
			cacheFb.setTimeToLive(8);
			cacheFb.setTimeToIdle(7);
			cacheFb.setDiskPersistent(true);
			cacheFb.setDiskExpiryThreadIntervalSeconds(10);
			cacheFb.afterPropertiesSet();
			cache = (Cache) cacheFb.getObject();
			config = cache.getCacheConfiguration();

			assertEquals("undefinedCache2", cache.getName());
			assertTrue("overridden maxElements is correct", config.getMaxElementsInMemory() == 5);
			assertFalse("overridden overflowToDisk is correct", config.isOverflowToDisk());
			assertTrue("overridden eternal is correct", config.isEternal());
			assertTrue("default timeToLive is correct", config.getTimeToLiveSeconds() == 8);
			assertTrue("default timeToIdle is correct", config.getTimeToIdleSeconds() == 7);
			assertTrue("overridden diskPersistent is correct", config.isDiskPersistent());
			assertTrue("overridden diskExpiryThreadIntervalSeconds is correct", config.getDiskExpiryThreadIntervalSeconds() == 10);
		}
		finally {
			if (useCacheManagerFb && cacheManagerFb != null) {
				cacheManagerFb.destroy();
			}
			else {
				CacheManager.getInstance().shutdown();
			}
		}
	}

	public void testEhCacheFactoryBeanWithBlockingCache() throws Exception {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			cacheFb.setCacheManager(cm);
			cacheFb.setCacheName("myCache1");
			cacheFb.setBlocking(true);
			assertEquals(cacheFb.getObjectType(), BlockingCache.class);
			cacheFb.afterPropertiesSet();
			Ehcache myCache1 = cm.getEhcache("myCache1");
			assertTrue(myCache1 instanceof BlockingCache);
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	public void testEhCacheFactoryBeanWithSelfPopulatingCache() throws Exception {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			cacheFb.setCacheManager(cm);
			cacheFb.setCacheName("myCache1");
			cacheFb.setCacheEntryFactory(new CacheEntryFactory() {
				public Object createEntry(Object key) throws Exception {
					return key;
				}
			});
			assertEquals(cacheFb.getObjectType(), SelfPopulatingCache.class);
			cacheFb.afterPropertiesSet();
			Ehcache myCache1 = cm.getEhcache("myCache1");
			assertTrue(myCache1 instanceof SelfPopulatingCache);
			assertEquals("myKey1", myCache1.get("myKey1").getValue());
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	public void testEhCacheFactoryBeanWithUpdatingSelfPopulatingCache() throws Exception {
		EhCacheManagerFactoryBean cacheManagerFb = new EhCacheManagerFactoryBean();
		cacheManagerFb.afterPropertiesSet();
		try {
			CacheManager cm = cacheManagerFb.getObject();
			EhCacheFactoryBean cacheFb = new EhCacheFactoryBean();
			cacheFb.setCacheManager(cm);
			cacheFb.setCacheName("myCache1");
			cacheFb.setCacheEntryFactory(new UpdatingCacheEntryFactory() {
				public Object createEntry(Object key) throws Exception {
					return key;
				}
				public void updateEntryValue(Object key, Object value) throws Exception {
				}
			});
			assertEquals(cacheFb.getObjectType(), UpdatingSelfPopulatingCache.class);
			cacheFb.afterPropertiesSet();
			Ehcache myCache1 = cm.getEhcache("myCache1");
			assertTrue(myCache1 instanceof UpdatingSelfPopulatingCache);
			assertEquals("myKey1", myCache1.get("myKey1").getValue());
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

}
