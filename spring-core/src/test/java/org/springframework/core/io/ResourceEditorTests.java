/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.io;

import java.beans.PropertyEditor;

import org.junit.Test;

import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link ResourceEditor} class.
 *
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Dave Syer
 */
public final class ResourceEditorTests {

	@Test
	public void sunnyDay() throws Exception {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText("classpath:org/springframework/core/io/ResourceEditorTests.class");
		Resource resource = (Resource) editor.getValue();
		assertNotNull(resource);
		assertTrue(resource.exists());
	}

	@Test(expected = IllegalArgumentException.class)
	public void ctorWithNullCtorArgs() throws Exception {
		new ResourceEditor(null, null);
	}

	@Test
	public void setAndGetAsTextWithNull() throws Exception {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText(null);
		assertEquals("", editor.getAsText());
	}

	@Test
	public void setAndGetAsTextWithWhitespaceResource() throws Exception {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText("  ");
		assertEquals("", editor.getAsText());
	}

	@Test
	public void testSystemPropertyReplacement() {
		PropertyEditor editor = new ResourceEditor();
		System.setProperty("test.prop", "foo");
		try {
			editor.setAsText("${test.prop}-${bar}");
			Resource resolved = (Resource) editor.getValue();
			assertEquals("foo-${bar}", resolved.getFilename());
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testStrictSystemPropertyReplacement() {
		PropertyEditor editor = new ResourceEditor(new DefaultResourceLoader(), new StandardEnvironment(), false);
		System.setProperty("test.prop", "foo");
		try {
			editor.setAsText("${test.prop}-${bar}");
			Resource resolved = (Resource) editor.getValue();
			assertEquals("foo-${bar}", resolved.getFilename());
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

}
