/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.cache.guava;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.junit.Test;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Biju Kunjummen
 */
public class GuavaCacheManagerTests {


	@Test
	public void testCreateCachesAndRetrieveKnownCache() {
		GuavaCacheManager cm = new GuavaCacheManager();
		Set<GuavaCache> setOfCaches = new HashSet<GuavaCache>();
		GuavaCacheFactoryBean cf1 = new GuavaCacheFactoryBean("c1");
		CacheBuilder<Object, Object> cb1 = CacheBuilder.newBuilder().maximumSize(10);
		cf1.setCacheBuilder(cb1);
		GuavaCache cache = cf1.getObject();
		setOfCaches.add(cache);
		cm.setCaches(setOfCaches);

		assertTrue(cm.getCache("c1") ==  cache);
	}

	@Test
	public void testCreateCachesAndRetrieveNewCache() {
		GuavaCacheManager cm = new GuavaCacheManager();
		Set<GuavaCache> setOfCaches = new HashSet<GuavaCache>();
		GuavaCacheFactoryBean cf1 = new GuavaCacheFactoryBean("c1");
		CacheBuilder<Object, Object> cb1 = CacheBuilder.newBuilder().maximumSize(10);
		cf1.setCacheBuilder(cb1);
		GuavaCache cache = cf1.getObject();
		setOfCaches.add(cache);
		cm.setCaches(setOfCaches);

		Cache c2 = cm.getCache("c2");

		assertEquals(c2.getName(), "c2");
	}



	@Test
	public void testWithCacheLoaderShouldReturnALoadingCache() {
		GuavaCacheFactoryBean cf1 = new GuavaCacheFactoryBean("c1");
		CacheBuilder<Object, Object> cb1 = CacheBuilder.newBuilder().maximumSize(10);
		cf1.setCacheBuilder(cb1);
		CacheLoader<Object,Object> loader = mockCacheLoader();
		cf1.setCacheLoader(loader);
		GuavaCache cache = cf1.getObject();

		assertTrue(cache.getNativeCache() instanceof LoadingCache);
	}




	@SuppressWarnings("unchecked")
	private CacheLoader<Object, Object> mockCacheLoader() {
		return mock(CacheLoader.class);
	}

}
