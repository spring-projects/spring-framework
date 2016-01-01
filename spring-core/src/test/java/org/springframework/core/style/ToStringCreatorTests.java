/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.style;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import org.springframework.util.ObjectUtils;

import static org.junit.Assert.*;

/**
 * @author Keith Donald
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class ToStringCreatorTests {

	private SomeObject s1, s2, s3;


	@Before
	public void setUp() throws Exception {
		s1 = new SomeObject() {
			@Override
			public String toString() {
				return "A";
			}
		};
		s2 = new SomeObject() {
			@Override
			public String toString() {
				return "B";
			}
		};
		s3 = new SomeObject() {
			@Override
			public String toString() {
				return "C";
			}
		};
	}

	@Test
	public void defaultStyleMap() {
		final Map map = getMap();
		Object stringy = new Object() {
			@Override
			public String toString() {
				return new ToStringCreator(this).append("familyFavoriteSport", map).toString();
			}
		};
		assertEquals("[ToStringCreatorTests.4@" + ObjectUtils.getIdentityHexString(stringy)
				+ " familyFavoriteSport = map['Keri' -> 'Softball', 'Scot' -> 'Fishing', 'Keith' -> 'Flag Football']]",
				stringy.toString());
	}

	private Map getMap() {
		Map map = new LinkedHashMap(3);
		map.put("Keri", "Softball");
		map.put("Scot", "Fishing");
		map.put("Keith", "Flag Football");
		return map;
	}

	@Test
	public void defaultStyleArray() {
		SomeObject[] array = new SomeObject[] { s1, s2, s3 };
		String str = new ToStringCreator(array).toString();
		assertEquals("[@" + ObjectUtils.getIdentityHexString(array)
				+ " array<ToStringCreatorTests.SomeObject>[A, B, C]]", str);
	}

	@Test
	public void primitiveArrays() {
		int[] integers = new int[] { 0, 1, 2, 3, 4 };
		String str = new ToStringCreator(integers).toString();
		assertEquals("[@" + ObjectUtils.getIdentityHexString(integers) + " array<Integer>[0, 1, 2, 3, 4]]", str);
	}

	@Test
	public void appendList() {
		List list = new ArrayList();
		list.add(s1);
		list.add(s2);
		list.add(s3);
		String str = new ToStringCreator(this).append("myLetters", list).toString();
		assertEquals("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = list[A, B, C]]",
				str);
	}

	@Test
	public void appendSet() {
		Set set = new LinkedHashSet<>(3);
		set.add(s1);
		set.add(s2);
		set.add(s3);
		String str = new ToStringCreator(this).append("myLetters", set).toString();
		assertEquals("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this) + " myLetters = set[A, B, C]]",
				str);
	}

	@Test
	public void appendClass() {
		String str = new ToStringCreator(this).append("myClass", this.getClass()).toString();
		assertEquals("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this)
				+ " myClass = ToStringCreatorTests]", str);
	}

	@Test
	public void appendMethod() throws Exception {
		String str = new ToStringCreator(this).append("myMethod", this.getClass().getMethod("appendMethod"))
				.toString();
		assertEquals("[ToStringCreatorTests@" + ObjectUtils.getIdentityHexString(this)
				+ " myMethod = appendMethod@ToStringCreatorTests]", str);
	}


	public static class SomeObject {
	}

}
