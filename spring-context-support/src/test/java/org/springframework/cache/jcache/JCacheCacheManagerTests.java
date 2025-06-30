/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.cache.jcache;

import java.util.ArrayList;
import java.util.List;

import javax.cache.Cache;
import javax.cache.CacheManager;

import org.junit.jupiter.api.BeforeEach;

import org.springframework.cache.transaction.AbstractTransactionSupportingCacheManagerTests;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Stephane Nicoll
 */
class JCacheCacheManagerTests extends AbstractTransactionSupportingCacheManagerTests<JCacheCacheManager> {

	private CacheManagerMock cacheManagerMock;

	private JCacheCacheManager cacheManager;

	private JCacheCacheManager transactionalCacheManager;


	@BeforeEach
	void setupOnce() {
		cacheManagerMock = new CacheManagerMock();
		cacheManagerMock.addCache(CACHE_NAME);

		cacheManager = new JCacheCacheManager(cacheManagerMock.getCacheManager());
		cacheManager.setTransactionAware(false);
		cacheManager.afterPropertiesSet();

		transactionalCacheManager = new JCacheCacheManager(cacheManagerMock.getCacheManager());
		transactionalCacheManager.setTransactionAware(true);
		transactionalCacheManager.afterPropertiesSet();
	}


	@Override
	protected JCacheCacheManager getCacheManager(boolean transactionAware) {
		if (transactionAware) {
			return transactionalCacheManager;
		}
		else {
			return cacheManager;
		}
	}

	@Override
	protected Class<? extends org.springframework.cache.Cache> getCacheType() {
		return JCacheCache.class;
	}

	@Override
	protected void addNativeCache(String cacheName) {
		cacheManagerMock.addCache(cacheName);
	}

	@Override
	protected void removeNativeCache(String cacheName) {
		cacheManagerMock.removeCache(cacheName);
	}


	private static class CacheManagerMock {

		private final List<String> cacheNames;

		private final CacheManager cacheManager = mock();

		private CacheManagerMock() {
			this.cacheNames = new ArrayList<>();
			given(cacheManager.getCacheNames()).willReturn(cacheNames);
		}

		private CacheManager getCacheManager() {
			return cacheManager;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		public void addCache(String name) {
			cacheNames.add(name);
			Cache cache = mock();
			given(cache.getName()).willReturn(name);
			given(cacheManager.getCache(name)).willReturn(cache);
		}

		public void removeCache(String name) {
			cacheNames.remove(name);
			given(cacheManager.getCache(name)).willReturn(null);
		}
	}

}
