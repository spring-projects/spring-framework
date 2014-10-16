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

package org.springframework.cache.guava.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.springframework.cache.guava.GuavaCache;
import org.springframework.cache.guava.GuavaCacheManager;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

public class GuavaCacheManagerConfigTests {

	@Test
	public void testLoadingAGuavaCacheManagerContext() {
		ConfigurableApplicationContext context = new GenericXmlApplicationContext(
				"/org/springframework/cache/guava/config/guavacache1.xml");

		GuavaCacheManager cm = context.getBean(GuavaCacheManager.class);
		GuavaCache c1 = context.getBean("c1", GuavaCache.class);
		GuavaCache c2 = context.getBean("c2", GuavaCache.class);
		GuavaCache c3 = context.getBean("c3", GuavaCache.class);
		GuavaCache c4 = context.getBean("c4", GuavaCache.class);

		assertTrue("Cache retrieved from CacheManager should be the same as the one in the context", cm.getCache("c1") == c1);
		assertTrue("Cache retrieved from CacheManager should be the same as the one in the context", cm.getCache("c2") == c2);
		assertTrue("Cache retrieved from CacheManager should be the same as the one in the context", cm.getCache("c3") == c3);
		assertTrue("Cache retrieved from CacheManager should be the same as the one in the context", cm.getCache("c4") == c4);
		assertEquals("c5", cm.getCache("c5").getName());

	}
}
