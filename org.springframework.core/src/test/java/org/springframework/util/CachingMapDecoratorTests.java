/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.util;

import junit.framework.TestCase;

/**
 * @author Keith Donald
 */
public class CachingMapDecoratorTests extends TestCase {

	public void testValidCache() {
		MyCachingMap cache = new MyCachingMap();
		Object value;

		value = cache.get("value key");
		assertTrue(cache.createCalled());
		assertEquals(value, "expensive value to cache");

		cache.get("value key 2");
		assertTrue(cache.createCalled());

		value = cache.get("value key");
		assertEquals(cache.createCalled(), false);
		assertEquals(value, "expensive value to cache");

		cache.get("value key 2");
		assertEquals(cache.createCalled(), false);
		assertEquals(value, "expensive value to cache");
	}


	private static class MyCachingMap extends CachingMapDecorator {

		private boolean createCalled;

		protected Object create(Object key) {
			createCalled = true;
			return "expensive value to cache";
		}

		public boolean createCalled() {
			boolean c = createCalled;
			this.createCalled = false;
			return c;
		}
	}

}
