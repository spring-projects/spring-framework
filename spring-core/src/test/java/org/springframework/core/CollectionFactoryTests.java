/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.core.CollectionFactory.*;

/**
 * Unit tests for {@link CollectionFactory}.
 *
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 4.1.4
 */
public class CollectionFactoryTests {

	@Test
	public void createsSimpleCollectionsCorrectly() {
		assertThat(createCollection(List.class, Object.class, 0), is(instanceOf(ArrayList.class)));
		assertThat(createCollection(Set.class, Object.class, 0), is(instanceOf(HashSet.class)));
		assertThat(createCollection(SortedSet.class, Object.class, 0), is(instanceOf(TreeSet.class)));
	}

	@Test
	public void createsEnumSet() {
		assertThat(createCollection(EnumSet.class, Color.class, 0), is(instanceOf(EnumSet.class)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidElementTypeForEnumSet() {
		createCollection(EnumSet.class, Object.class, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullElementTypeForEnumSet() {
		createCollection(EnumSet.class, null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullCollectionType() {
		createCollection(null, Object.class, 0);
	}

	@Test
	public void createsEnumMap() {
		assertThat(createMap(EnumMap.class, Color.class, 0), is(instanceOf(EnumMap.class)));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsInvalidKeyTypeForEnumMap() {
		createMap(EnumMap.class, Object.class, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullKeyTypeForEnumMap() {
		createMap(EnumMap.class, null, 0);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullMapType() {
		createMap(null, Object.class, 0);
	}


	static enum Color {
		RED, BLUE;
	}
}
