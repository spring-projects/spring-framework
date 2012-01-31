/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import junit.framework.TestCase;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class StringArrayPropertyEditorTests extends TestCase {

	public void testWithDefaultSeparator() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText("0,1,2");
		Object value = editor.getValue();
		assertNotNull(value);
		assertTrue(value instanceof String[]);
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertEquals("" + i, array[i]);
		}
		assertEquals("0,1,2", editor.getAsText());
	}

	public void testTrimByDefault() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText(" 0,1 , 2 ");
		Object value = editor.getValue();
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertEquals("" + i, array[i]);
		}
		assertEquals("0,1,2", editor.getAsText());
	}

	public void testNoTrim() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",",false,false);
		editor.setAsText("  0,1  , 2 ");
		Object value = editor.getValue();
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertEquals(3, array[i].length());
			assertEquals("" + i, array[i].trim());
		}
		assertEquals("  0,1  , 2 ", editor.getAsText());
	}

	public void testWithCustomSeparator() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(":");
		editor.setAsText("0:1:2");
		Object value = editor.getValue();
		assertTrue(value instanceof String[]);
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertEquals("" + i, array[i]);
		}
		assertEquals("0:1:2", editor.getAsText());
	}

	public void testWithCharsToDelete() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", "\r\n", false);
		editor.setAsText("0\r,1,\n2");
		Object value = editor.getValue();
		assertTrue(value instanceof String[]);
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertEquals("" + i, array[i]);
		}
		assertEquals("0,1,2", editor.getAsText());
	}

	public void testWithEmptyArray() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText("");
		Object value = editor.getValue();
		assertTrue(value instanceof String[]);
		assertEquals(0, ((String[]) value).length);
	}

	public void testWithEmptyArrayAsNull() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", true);
		editor.setAsText("");
		assertNull(editor.getValue());
	}

}
