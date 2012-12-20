/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.springframework.util.MultiValueMap;

/**
 * @author Darren Davison
 * @author Juergen Hoeller
 * @author Dave Syer
 */
@SuppressWarnings({"deprecation", "rawtypes"})
public class CollectionFactoryTests extends TestCase {

	public void testLinkedSet() {
		Set set = CollectionFactory.createLinkedSetIfPossible(16);
		assertTrue(set instanceof LinkedHashSet);
	}

	public void testLinkedMap() {
		Map map = CollectionFactory.createLinkedMapIfPossible(16);
		assertTrue(map instanceof LinkedHashMap);
	}

	public void testIdentityMap() {
		Map map = CollectionFactory.createIdentityMapIfPossible(16);
		assertTrue(map instanceof IdentityHashMap);
	}

	public void testConcurrentMap() {
		Map map = CollectionFactory.createConcurrentMapIfPossible(16);
		assertTrue(map.getClass().getName().endsWith("ConcurrentHashMap"));
	}

	public void testMultiValueMap() {
		Map map = CollectionFactory.createMap(MultiValueMap.class, 16);
		assertTrue(map.getClass().getName().endsWith("MultiValueMap"));
	}

	public void testConcurrentMapWithExplicitInterface() {
		ConcurrentMap map = CollectionFactory.createConcurrentMap(16);
		assertTrue(map.getClass().getSuperclass().getName().endsWith("ConcurrentHashMap"));
		map.putIfAbsent("key", "value1");
		map.putIfAbsent("key", "value2");
		assertEquals("value1", map.get("key"));
	}

}
