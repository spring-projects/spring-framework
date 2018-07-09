/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.core.env;

import java.util.Iterator;

import org.junit.Test;

import org.springframework.mock.env.MockPropertySource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * @author Chris Beams
 * @author Juergen Hoeller
 */
public class MutablePropertySourcesTests {

	@Test
	public void test() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new MockPropertySource("b").withProperty("p1", "bValue"));
		sources.addLast(new MockPropertySource("d").withProperty("p1", "dValue"));
		sources.addLast(new MockPropertySource("f").withProperty("p1", "fValue"));

		assertThat(sources.size(), equalTo(3));
		assertThat(sources.contains("a"), is(false));
		assertThat(sources.contains("b"), is(true));
		assertThat(sources.contains("c"), is(false));
		assertThat(sources.contains("d"), is(true));
		assertThat(sources.contains("e"), is(false));
		assertThat(sources.contains("f"), is(true));
		assertThat(sources.contains("g"), is(false));

		assertThat(sources.get("b"), not(nullValue()));
		assertThat(sources.get("b").getProperty("p1"), equalTo("bValue"));
		assertThat(sources.get("d"), not(nullValue()));
		assertThat(sources.get("d").getProperty("p1"), equalTo("dValue"));

		sources.addBefore("b", new MockPropertySource("a"));
		sources.addAfter("b", new MockPropertySource("c"));

		assertThat(sources.size(), equalTo(5));
		assertThat(sources.precedenceOf(PropertySource.named("a")), is(0));
		assertThat(sources.precedenceOf(PropertySource.named("b")), is(1));
		assertThat(sources.precedenceOf(PropertySource.named("c")), is(2));
		assertThat(sources.precedenceOf(PropertySource.named("d")), is(3));
		assertThat(sources.precedenceOf(PropertySource.named("f")), is(4));

		sources.addBefore("f", new MockPropertySource("e"));
		sources.addAfter("f", new MockPropertySource("g"));

		assertThat(sources.size(), equalTo(7));
		assertThat(sources.precedenceOf(PropertySource.named("a")), is(0));
		assertThat(sources.precedenceOf(PropertySource.named("b")), is(1));
		assertThat(sources.precedenceOf(PropertySource.named("c")), is(2));
		assertThat(sources.precedenceOf(PropertySource.named("d")), is(3));
		assertThat(sources.precedenceOf(PropertySource.named("e")), is(4));
		assertThat(sources.precedenceOf(PropertySource.named("f")), is(5));
		assertThat(sources.precedenceOf(PropertySource.named("g")), is(6));

		sources.addLast(new MockPropertySource("a"));
		assertThat(sources.size(), equalTo(7));
		assertThat(sources.precedenceOf(PropertySource.named("b")), is(0));
		assertThat(sources.precedenceOf(PropertySource.named("c")), is(1));
		assertThat(sources.precedenceOf(PropertySource.named("d")), is(2));
		assertThat(sources.precedenceOf(PropertySource.named("e")), is(3));
		assertThat(sources.precedenceOf(PropertySource.named("f")), is(4));
		assertThat(sources.precedenceOf(PropertySource.named("g")), is(5));
		assertThat(sources.precedenceOf(PropertySource.named("a")), is(6));

		sources.addFirst(new MockPropertySource("a"));
		assertThat(sources.size(), equalTo(7));
		assertThat(sources.precedenceOf(PropertySource.named("a")), is(0));
		assertThat(sources.precedenceOf(PropertySource.named("b")), is(1));
		assertThat(sources.precedenceOf(PropertySource.named("c")), is(2));
		assertThat(sources.precedenceOf(PropertySource.named("d")), is(3));
		assertThat(sources.precedenceOf(PropertySource.named("e")), is(4));
		assertThat(sources.precedenceOf(PropertySource.named("f")), is(5));
		assertThat(sources.precedenceOf(PropertySource.named("g")), is(6));

		assertEquals(sources.remove("a"), PropertySource.named("a"));
		assertThat(sources.size(), equalTo(6));
		assertThat(sources.contains("a"), is(false));

		assertNull(sources.remove("a"));
		assertThat(sources.size(), equalTo(6));

		String bogusPS = "bogus";
		try {
			sources.addAfter(bogusPS, new MockPropertySource("h"));
			fail("expected non-existent PropertySource exception");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("does not exist"));
		}

		sources.addFirst(new MockPropertySource("a"));
		assertThat(sources.size(), equalTo(7));
		assertThat(sources.precedenceOf(PropertySource.named("a")), is(0));
		assertThat(sources.precedenceOf(PropertySource.named("b")), is(1));
		assertThat(sources.precedenceOf(PropertySource.named("c")), is(2));

		sources.replace("a", new MockPropertySource("a-replaced"));
		assertThat(sources.size(), equalTo(7));
		assertThat(sources.precedenceOf(PropertySource.named("a-replaced")), is(0));
		assertThat(sources.precedenceOf(PropertySource.named("b")), is(1));
		assertThat(sources.precedenceOf(PropertySource.named("c")), is(2));

		sources.replace("a-replaced", new MockPropertySource("a"));

		try {
			sources.replace(bogusPS, new MockPropertySource("bogus-replaced"));
			fail("expected non-existent PropertySource exception");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("does not exist"));
		}

		try {
			sources.addBefore("b", new MockPropertySource("b"));
			fail("expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("cannot be added relative to itself"));
		}

		try {
			sources.addAfter("b", new MockPropertySource("b"));
			fail("expected exception");
		}
		catch (IllegalArgumentException ex) {
			assertTrue(ex.getMessage().contains("cannot be added relative to itself"));
		}
	}

	@Test
	public void getNonExistentPropertySourceReturnsNull() {
		MutablePropertySources sources = new MutablePropertySources();
		assertThat(sources.get("bogus"), nullValue());
	}

	@Test
	public void iteratorContainsPropertySource() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new MockPropertySource("test"));

		Iterator<PropertySource<?>> it = sources.iterator();
		assertTrue(it.hasNext());
		assertEquals("test", it.next().getName());

		try {
			it.remove();
			fail("Should have thrown UnsupportedOperationException");
		}
		catch (UnsupportedOperationException ex) {
			// expected
		}
		assertFalse(it.hasNext());
	}

	@Test
	public void iteratorIsEmptyForEmptySources() {
		MutablePropertySources sources = new MutablePropertySources();
		Iterator<PropertySource<?>> it = sources.iterator();
		assertFalse(it.hasNext());
	}

	@Test
	public void streamContainsPropertySource() {
		MutablePropertySources sources = new MutablePropertySources();
		sources.addLast(new MockPropertySource("test"));

		assertThat(sources.stream(), notNullValue());
		assertThat(sources.stream().count(), is(1L));
		assertThat(sources.stream().anyMatch(source -> "test".equals(source.getName())), is(true));
		assertThat(sources.stream().anyMatch(source -> "bogus".equals(source.getName())), is(false));
	}

	@Test
	public void streamIsEmptyForEmptySources() {
		MutablePropertySources sources = new MutablePropertySources();
		assertThat(sources.stream(), notNullValue());
		assertThat(sources.stream().count(), is(0L));
	}

}
