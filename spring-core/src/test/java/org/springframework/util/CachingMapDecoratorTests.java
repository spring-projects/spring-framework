/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class CachingMapDecoratorTests extends TestCase {

	public void testValidCache() {
		MyCachingMap cache = new MyCachingMap();

		assertFalse(cache.containsKey("value key"));
		assertFalse(cache.containsValue("expensive value to cache"));
		Object value = cache.get("value key");
		assertTrue(cache.createCalled());
		assertEquals(value, "expensive value to cache");
		assertTrue(cache.containsKey("value key"));
		assertTrue(cache.containsValue("expensive value to cache"));

		assertFalse(cache.containsKey("value key 2"));
		value = cache.get("value key 2");
		assertTrue(cache.createCalled());
		assertEquals(value, "expensive value to cache");
		assertTrue(cache.containsKey("value key 2"));

		value = cache.get("value key");
		assertFalse(cache.createCalled());
		assertEquals(value, "expensive value to cache");

		cache.get("value key 2");
		assertFalse(cache.createCalled());
		assertEquals(value, "expensive value to cache");

		assertFalse(cache.containsKey(null));
		assertFalse(cache.containsValue(null));
		value = cache.get(null);
		assertTrue(cache.createCalled());
		assertNull(value);
		assertTrue(cache.containsKey(null));
		assertTrue(cache.containsValue(null));

		value = cache.get(null);
		assertFalse(cache.createCalled());
		assertNull(value);

		Set<String> keySet = cache.keySet();
		assertEquals(3, keySet.size());
		assertTrue(keySet.contains("value key"));
		assertTrue(keySet.contains("value key 2"));
		assertTrue(keySet.contains(null));

		Collection<String> values = cache.values();
		assertEquals(3, values.size());
		assertTrue(values.contains("expensive value to cache"));
		assertTrue(values.contains(null));

		Set<Map.Entry<String, String>> entrySet = cache.entrySet();
		assertEquals(3, entrySet.size());
		keySet = new HashSet<String>();
		values = new HashSet<String>();
		for (Map.Entry<String, String> entry : entrySet) {
			keySet.add(entry.getKey());
			values.add(entry.getValue());
		}
		assertTrue(keySet.contains("value key"));
		assertTrue(keySet.contains("value key 2"));
		assertTrue(keySet.contains(null));
		assertEquals(2, values.size());
		assertTrue(values.contains("expensive value to cache"));
		assertTrue(values.contains(null));
	}


	@SuppressWarnings("serial")
	private static class MyCachingMap extends CachingMapDecorator<String, String> {

		private boolean createCalled;

		protected String create(String key) {
			createCalled = true;
			return (key != null ? "expensive value to cache" : null);
		}

		public boolean createCalled() {
			boolean c = createCalled;
			this.createCalled = false;
			return c;
		}
	}

}
