/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

import java.beans.PropertyEditor;

/**
 * Unit tests for the {@link ResourceEditor} class.
 *
 * @author Rick Evans
 */
public final class ResourceEditorTests extends TestCase {

	public void testSunnyDay() throws Exception {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText("classpath:org/springframework/core/io/ResourceEditorTests.class");
		Resource resource = (Resource) editor.getValue();
		assertNotNull(resource);
		assertTrue(resource.exists());
	}

	public void testCtorWithNullResourceLoader() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new ResourceEditor(null);
			}
		}.runTest();
	}

	public void testSetAndGetAsTextWithNull() throws Exception {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText(null);
		assertEquals("", editor.getAsText());
	}

	public void testSetAndGetAsTextWithWhitespaceResource() throws Exception {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText("  ");
		assertEquals("", editor.getAsText());
	}

}
