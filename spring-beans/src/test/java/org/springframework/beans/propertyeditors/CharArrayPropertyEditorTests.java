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

package org.springframework.beans.propertyeditors;

import junit.framework.TestCase;

import java.beans.PropertyEditor;

/**
 * Unit tests for the {@link CharArrayPropertyEditor} class.
 *
 * @author Rick Evans
 */
public final class CharArrayPropertyEditorTests extends TestCase {

	public void testSunnyDaySetAsText() throws Exception {
		final String text = "Hideous towns make me throw... up";

		PropertyEditor charEditor = new CharArrayPropertyEditor();
		charEditor.setAsText(text);

		Object value = charEditor.getValue();
		assertNotNull(value);
		assertTrue(value instanceof char[]);
		char[] chars = (char[]) value;
		for (int i = 0; i < text.length(); ++i) {
			assertEquals("char[] differs at index '" + i + "'", text.charAt(i), chars[i]);
		}
		assertEquals(text, charEditor.getAsText());
	}

	public void testGetAsTextReturnsEmptyStringIfValueIsNull() throws Exception {
		PropertyEditor charEditor = new CharArrayPropertyEditor();
		assertEquals("", charEditor.getAsText());

		charEditor.setAsText(null);
		assertEquals("", charEditor.getAsText());
	}

}
