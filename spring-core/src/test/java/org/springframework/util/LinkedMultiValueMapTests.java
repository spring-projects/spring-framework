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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 */
public class LinkedMultiValueMapTests {

	private LinkedMultiValueMap<String, String> map;

	@Before
	public void setUp() {
		map = new LinkedMultiValueMap<String, String>();
	}

	@Test
	public void add() {
		map.add("key", "value1");
		map.add("key", "value2");
		assertEquals(1, map.size());
		List<String> expected = new ArrayList<String>(2);
		expected.add("value1");
		expected.add("value2");
		assertEquals(expected, map.get("key"));
	}

	@Test
	public void getFirst() {
		List<String> values = new ArrayList<String>(2);
		values.add("value1");
		values.add("value2");
		map.put("key", values);
		assertEquals("value1", map.getFirst("key"));
		assertNull(map.getFirst("other"));
	}

	@Test
	public void set() {
		map.set("key", "value1");
		map.set("key", "value2");
		assertEquals(1, map.size());
		assertEquals(Collections.singletonList("value2"), map.get("key"));
	}

	@Test
	public void equals() {
		map.set("key1", "value1");
		assertEquals(map, map);
		MultiValueMap<String, String> o1 = new LinkedMultiValueMap<String, String>();
		o1.set("key1", "value1");
		assertEquals(map, o1);
		assertEquals(o1, map);
		Map<String, List<String>> o2 = new HashMap<String, List<String>>();
		o2.put("key1", Collections.singletonList("value1"));
		assertEquals(map, o2);
		assertEquals(o2, map);
	}

}
