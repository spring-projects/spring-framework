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

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cache.AbstractCacheTests;
import org.springframework.tests.EnabledForTestGroups;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.tests.TestGroup.LONG_RUNNING;

/**
 * @author Costin Leau
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class EhCacheCacheTests extends AbstractCacheTests<EhCacheCache> {

	private CacheManager cacheManager;

	private Ehcache nativeCache;

	private EhCacheCache cache;


	@BeforeEach
	public void setup() {
		cacheManager = new CacheManager(new Configuration().name("EhCacheCacheTests")
				.defaultCache(new CacheConfiguration("default", 100)));
		nativeCache = new net.sf.ehcache.Cache(new CacheConfiguration(CACHE_NAME, 100));
		cacheManager.addCache(nativeCache);

		cache = new EhCacheCache(nativeCache);
	}

	@AfterEach
	public void shutdown() {
		cacheManager.shutdown();
	}


	@Override
	protected EhCacheCache getCache() {
		return cache;
	}

	@Override
	protected Ehcache getNativeCache() {
		return nativeCache;
	}


	@Test
	@EnabledForTestGroups(LONG_RUNNING)
	public void testExpiredElements() throws Exception {
		String key = "brancusi";
		String value = "constantin";
		Element brancusi = new Element(key, value);
		// ttl = 10s
		brancusi.setTimeToLive(3);
		nativeCache.put(brancusi);

		assertThat(cache.get(key).get()).isEqualTo(value);
		// wait for the entry to expire
		Thread.sleep(5 * 1000);
		assertThat(cache.get(key)).isNull();
	}

}
