/*
 * Copyright 2010 the original author or authors.
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

import net.sf.ehcache.Ehcache;

import org.springframework.cache.Cache;
import org.springframework.cache.vendor.AbstractNativeCacheTest;

/**
 * Integration test for EhCache cache.
 * 
 * @author Costin Leau
 */
public class EhCacheCacheTest extends AbstractNativeCacheTest<Ehcache> {

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
}
