/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.cache.infinispan;

import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Philippe Marschall
 */
public class InfinispanSupportTests {

	@Test
	public void testLoadingBlankCacheManager() throws Exception {
		InfinispanCacheManagerFactoryBean cacheManagerFb = new InfinispanCacheManagerFactoryBean();
		assertEquals(EmbeddedCacheManager.class, cacheManagerFb.getObjectType());
		assertTrue("Singleton property", cacheManagerFb.isSingleton());
		cacheManagerFb.afterPropertiesSet();
		try {
			EmbeddedCacheManager cm = cacheManagerFb.getObject();
			assertThat("Loaded CacheManager with no caches", cm.getCacheNames(), empty());
			org.infinispan.Cache myCache1 = cm.getCache("myCache1", false);
			assertNull("No myCache1 defined", myCache1);
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

	@Test
	public void testLoadingCacheManagerFromConfigFile() throws Exception {
		InfinispanCacheManagerFactoryBean cacheManagerFb = new InfinispanCacheManagerFactoryBean();
		cacheManagerFb.setConfigLocation(new ClassPathResource("testInfinispan.xml", getClass()));
		cacheManagerFb.afterPropertiesSet();
		try {
			EmbeddedCacheManager cm = cacheManagerFb.getObject();
			assertThat("Correct number of caches loaded", cm.getCacheNames(), hasSize(1));
			org.infinispan.Cache myCache1 = cm.getCache("myCache1");
			assertNotNull("No myCache1 defined", myCache1);
			assertEquals("myCache1 is not LIRS", myCache1.getCacheConfiguration().eviction().strategy(), EvictionStrategy.LIRS);
			assertTrue("myCache1.maxElements == 300", myCache1.getCacheConfiguration().eviction().maxEntries() == 300);
		}
		finally {
			cacheManagerFb.destroy();
		}
	}

}
