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

package org.springframework.core.io.support;

import java.beans.PropertyEditor;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.core.io.Resource;

/**
 * @author Dave Syer
 * @author Juergen Hoeller
 */
public class ResourceArrayPropertyEditorTests {

	@Test
	public void testVanillaResource() throws Exception {
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath:org/springframework/core/io/support/ResourceArrayPropertyEditor.class");
		Resource[] resources = (Resource[]) editor.getValue();
		assertNotNull(resources);
		assertTrue(resources[0].exists());
	}

	@Test
	public void testPatternResource() throws Exception {
		// N.B. this will sometimes fail if you use classpath: instead of classpath*:.
		// The result depends on the classpath - if test-classes are segregated from classes
		// and they come first on the classpath (like in Maven) then it breaks, if classes
		// comes first (like in Spring Build) then it is OK.
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		editor.setAsText("classpath*:org/springframework/core/io/support/Resource*Editor.class");
		Resource[] resources = (Resource[]) editor.getValue();
		assertNotNull(resources);
		assertTrue(resources[0].exists());
	}

	@Test
	public void testSystemPropertyReplacement() {
		PropertyEditor editor = new ResourceArrayPropertyEditor();
		System.setProperty("test.prop", "foo");
		try {
			editor.setAsText("${test.prop}-${bar}");
			Resource[] resources = (Resource[]) editor.getValue();
			assertEquals("foo-${bar}", resources[0].getFilename());
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

	@Test(expected=IllegalArgumentException.class)
	public void testStrictSystemPropertyReplacement() {
		PropertyEditor editor = new ResourceArrayPropertyEditor(new PathMatchingResourcePatternResolver(), false);
		System.setProperty("test.prop", "foo");
		try {
			editor.setAsText("${test.prop}-${bar}");
			Resource[] resources = (Resource[]) editor.getValue();
			assertEquals("foo-${bar}", resources[0].getFilename());
		}
		finally {
			System.getProperties().remove("test.prop");
		}
	}

}
