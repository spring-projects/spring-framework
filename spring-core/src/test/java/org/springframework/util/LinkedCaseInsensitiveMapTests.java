/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class LinkedCaseInsensitiveMapTests {

	private final LinkedCaseInsensitiveMap<String> map = new LinkedCaseInsensitiveMap<String>();


	@Test
	public void putAndGet() {
		map.put("key", "value1");
		map.put("key", "value2");
		map.put("key", "value3");
		assertEquals(1, map.size());
		assertEquals("value3", map.get("key"));
		assertEquals("value3", map.get("KEY"));
		assertEquals("value3", map.get("Key"));
	}

	@Test
	public void putWithOverlappingKeys() {
		map.put("key", "value1");
		map.put("KEY", "value2");
		map.put("Key", "value3");
		assertEquals(1, map.size());
		assertEquals("value3", map.get("key"));
		assertEquals("value3", map.get("KEY"));
		assertEquals("value3", map.get("Key"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void mapClone() {
		map.put("key", "value1");
		LinkedCaseInsensitiveMap<String> copy = (LinkedCaseInsensitiveMap<String>) map.clone();
		assertEquals("value1", map.get("key"));
		assertEquals("value1", map.get("KEY"));
		assertEquals("value1", map.get("Key"));
		assertEquals("value1", copy.get("key"));
		assertEquals("value1", copy.get("KEY"));
		assertEquals("value1", copy.get("Key"));
		copy.put("Key", "value2");
		assertEquals(1, map.size());
		assertEquals(1, copy.size());
		assertEquals("value1", map.get("key"));
		assertEquals("value1", map.get("KEY"));
		assertEquals("value1", map.get("Key"));
		assertEquals("value2", copy.get("key"));
		assertEquals("value2", copy.get("KEY"));
		assertEquals("value2", copy.get("Key"));
	}

}
