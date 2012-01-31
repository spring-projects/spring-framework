/*
 * Copyright 2010-2011 the original author or authors.
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

import static org.junit.Assert.*;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import org.junit.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.vendor.AbstractNativeCacheTests;

/**
 * Integration test for EhCache cache.
 * 
 * @author Costin Leau
 */
public class EhCacheCacheTests extends AbstractNativeCacheTests<Ehcache> {

	@Override
	protected Ehcache createNativeCache() throws Exception {
		EhCacheFactoryBean fb = new EhCacheFactoryBean();
		fb.setBeanName(CACHE_NAME);
		fb.setCacheName(CACHE_NAME);
		fb.afterPropertiesSet();
		return fb.getObject();
	}

	@Override
	protected Cache createCache(Ehcache nativeCache) {
		return new EhCacheCache(nativeCache);
	}

	@Test
	public void testExpiredElements() throws Exception {
		String key = "brancusi";
		String value = "constantin";
		Element brancusi = new Element(key, value);
		// ttl = 10s
		brancusi.setTimeToLive(3);
		nativeCache.put(brancusi);

		assertEquals(value, cache.get(key).get());
		// wait for the entry to expire
		Thread.sleep(5 * 1000);
		assertNull(cache.get(key));
	}
}