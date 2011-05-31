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

import org.junit.Test;

/**
 * Unit tests for {@link ContextLoaderUtils}.
 * 
 * @author Sam Brannen
 * @since 3.1
 */
public class ContextLoaderUtilsTests {

	@Test
	public void resolveActivatedProfilesWithoutAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActivatedProfiles(Enigma.class);
		assertNotNull(profiles);
		assertEquals(0, profiles.length);
	}

	@Test
	public void resolveActivatedProfilesWithNoProfilesDeclared() {
		String[] profiles = ContextLoaderUtils.resolveActivatedProfiles(NoProfilesDeclared.class);
		assertNotNull(profiles);
		assertEquals(0, profiles.length);
	}

	@Test
	public void resolveActivatedProfilesWithEmptyProfiles() {
		String[] profiles = ContextLoaderUtils.resolveActivatedProfiles(EmptyProfiles.class);
		assertNotNull(profiles);
		assertEquals(0, profiles.length);
	}

	@Test
	public void resolveActivatedProfilesWithLocalAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActivatedProfiles(Foo.class);
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertEquals("foo", profiles[0]);
	}

	@Test
	public void resolveActivatedProfilesWithInheritedAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActivatedProfiles(InheritedFoo.class);
		assertNotNull(profiles);
		assertEquals(1, profiles.length);
		assertEquals("foo", profiles[0]);
	}

	@Test
	public void resolveActivatedProfilesWithLocalAndInheritedAnnotations() {
		String[] profiles = ContextLoaderUtils.resolveActivatedProfiles(Bar.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);
		assertEquals("foo", profiles[0]);
		assertEquals("bar", profiles[1]);
	}

	@Test
	public void resolveActivatedProfilesWithOverriddenAnnotation() {
		String[] profiles = ContextLoaderUtils.resolveActivatedProfiles(Animals.class);
		assertNotNull(profiles);
		assertEquals(2, profiles.length);
		assertEquals("dog", profiles[0]);
		assertEquals("cat", profiles[1]);
	}


	private static class Enigma {
	}

	@ActivateProfiles
	private static class NoProfilesDeclared {
	}

	@ActivateProfiles({ "    ", "\t" })
	private static class EmptyProfiles {
	}

	@ActivateProfiles(profiles = "foo")
	private static class Foo {
	}

	private static class InheritedFoo extends Foo {
	}

	@ActivateProfiles("bar")
	private static class Bar extends Foo {
	}

	@ActivateProfiles(profiles = { "dog", "cat" }, inheritProfiles = false)
	private static class Animals extends Bar {
	}

}
