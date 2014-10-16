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
import org.mockito.Mockito;

import static org.junit.Assert.*;

/**
 * @author Biju Kunjummen
 */

public class GuavaCacheFactoryBeanTests {

	@Test
	public void testDefaultCacheBuilder() {
		GuavaCacheFactoryBean cb1 = new GuavaCacheFactoryBean("cb1");
		GuavaCache guavaCache = cb1.getObject();
		assertNotNull(guavaCache);
		assertNotNull(guavaCache.getNativeCache());
		assertEquals("cb1", guavaCache.getName());
	}

	@Test
	public void testCacheBuilderExplicitlySpecified() {
		GuavaCacheFactoryBean cb1 = new GuavaCacheFactoryBean("cb1");
		cb1.setCacheBuilder(CacheBuilder.newBuilder().maximumSize(1));
		GuavaCache guavaCache = cb1.getObject();
		assertNotNull(guavaCache);
		assertNotNull(guavaCache.getNativeCache());
		assertEquals("cb1", guavaCache.getName());
	}

	@Test
	public void testCacheBuilderAndCacheLoaderExplicitlySpecified() {
		GuavaCacheFactoryBean cb1 = new GuavaCacheFactoryBean("cb1");
		cb1.setCacheBuilder(CacheBuilder.newBuilder().maximumSize(1));
		cb1.setCacheLoader(Mockito.mock(CacheLoader.class));
		GuavaCache guavaCache = cb1.getObject();
		assertNotNull(guavaCache);
		assertTrue(guavaCache.getNativeCache() instanceof LoadingCache);

	}
}
