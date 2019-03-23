/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans;

import java.util.Arrays;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class PropertyAccessorUtilsTests {

	@Test
	public void testCanonicalPropertyName() {
		assertEquals("map", PropertyAccessorUtils.canonicalPropertyName("map"));
		assertEquals("map[key1]", PropertyAccessorUtils.canonicalPropertyName("map[key1]"));
		assertEquals("map[key1]", PropertyAccessorUtils.canonicalPropertyName("map['key1']"));
		assertEquals("map[key1]", PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"]"));
		assertEquals("map[key1][key2]", PropertyAccessorUtils.canonicalPropertyName("map[key1][key2]"));
		assertEquals("map[key1][key2]", PropertyAccessorUtils.canonicalPropertyName("map['key1'][\"key2\"]"));
		assertEquals("map[key1].name", PropertyAccessorUtils.canonicalPropertyName("map[key1].name"));
		assertEquals("map[key1].name", PropertyAccessorUtils.canonicalPropertyName("map['key1'].name"));
		assertEquals("map[key1].name", PropertyAccessorUtils.canonicalPropertyName("map[\"key1\"].name"));
	}

	@Test
	public void testCanonicalPropertyNames() {
		String[] original =
				new String[] {"map", "map[key1]", "map['key1']", "map[\"key1\"]", "map[key1][key2]",
											"map['key1'][\"key2\"]", "map[key1].name", "map['key1'].name", "map[\"key1\"].name"};
		String[] canonical =
				new String[] {"map", "map[key1]", "map[key1]", "map[key1]", "map[key1][key2]",
											"map[key1][key2]", "map[key1].name", "map[key1].name", "map[key1].name"};

		assertTrue(Arrays.equals(canonical, PropertyAccessorUtils.canonicalPropertyNames(original)));
	}

}
