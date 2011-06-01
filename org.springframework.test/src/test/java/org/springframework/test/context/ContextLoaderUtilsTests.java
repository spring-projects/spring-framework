/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.test.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Unit tests for {@link ContextLoaderUtils}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class ContextLoaderUtilsTests {

	@Test
	public void resolveActiveProfilesWithoutAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Enigma.class);
		assertNotNull(profiles);
		assertEquals(0, profiles.length);
	}

	@Test
	public void resolveActiveProfilesWithNoProfilesDeclared() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(NoProfilesDeclared.class);
		assertNotNull(profiles);
		assertEquals(0, profiles.length);
	}

	@Test
	public void resolveActiveProfilesWithEmptyProfiles() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(EmptyProfiles.class);
		assertNotNull(profiles);
		assertEquals(0, profiles.length);
	}

	@Test
	public void resolveActiveProfilesWithDuplicatedProfiles() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(DuplicatedProfiles.class);
		assertNotNull(profiles);
		assertEquals(3, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));
		assertTrue(list.contains("baz"));
	}

	@Test
	public void resolveActiveProfilesWithLocalAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Foo.class);
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertEquals("foo", profiles[0]);
	}

	@Test
	public void resolveActiveProfilesWithInheritedAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(InheritedFoo.class);
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertEquals("foo", profiles[0]);
	}

	@Test
	public void resolveActiveProfilesWithLocalAndInheritedAnnotations() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Bar.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("foo"));
		assertTrue(list.contains("bar"));
	}

	@Test
	public void resolveActiveProfilesWithOverriddenAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActiveProfiles(Animals.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);

		List<String> list = Arrays.asList(profiles);
		assertTrue(list.contains("dog"));
		assertTrue(list.contains("cat"));
	}


	private static class Enigma {
	}

	@ActiveProfiles
	private static class NoProfilesDeclared {
	}

	@ActiveProfiles({ "    ", "\t" })
	private static class EmptyProfiles {
	}

	@ActiveProfiles({ "foo", "bar", "foo", "bar", "baz" })
	private static class DuplicatedProfiles {
	}

	@ActiveProfiles(profiles = "foo")
	private static class Foo {
	}

	private static class InheritedFoo extends Foo {
	}

	@ActiveProfiles("bar")
	private static class Bar extends Foo {
	}

	@ActiveProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	private static class Animals extends Bar {
	}

}
