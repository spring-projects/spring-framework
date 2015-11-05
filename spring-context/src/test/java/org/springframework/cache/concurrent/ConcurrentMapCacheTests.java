/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.cache.concurrent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Ignore;

import org.springframework.cache.AbstractCacheTests;

/**
 * @author Costin Leau
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 */
public class ConcurrentMapCacheTests extends AbstractCacheTests<ConcurrentMapCache> {

	protected ConcurrentMap<Object, Object> nativeCache;

	protected ConcurrentMapCache cache;


	@Before
	public void setUp() throws Exception {
		nativeCache = new ConcurrentHashMap<Object, Object>();
		cache = new ConcurrentMapCache(CACHE_NAME, nativeCache, true);
		cache.clear();
	}

	@Override
	protected ConcurrentMapCache getCache() {
		return this.cache;
	}

	@Override
	protected ConcurrentMap<Object, Object> getNativeCache() {
		return this.nativeCache;
	}

}
