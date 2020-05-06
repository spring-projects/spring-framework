/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
class StringArrayPropertyEditorTests {

	@Test
	void withDefaultSeparator() {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText("0,1,2");
		Object value = editor.getValue();
		assertTrimmedElements(value);
		assertThat(editor.getAsText()).isEqualTo("0,1,2");
	}

	@Test
	void trimByDefault() {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText(" 0,1 , 2 ");
		Object value = editor.getValue();
		assertTrimmedElements(value);
		assertThat(editor.getAsText()).isEqualTo("0,1,2");
	}

	@Test
	void noTrim() {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", false, false);
		editor.setAsText("  0,1  , 2 ");
		Object value = editor.getValue();
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertThat(array[i].length()).isEqualTo(3);
			assertThat(array[i].trim()).isEqualTo(("" + i));
		}
		assertThat(editor.getAsText()).isEqualTo("  0,1  , 2 ");
	}

	@Test
	void withCustomSeparator() {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(":");
		editor.setAsText("0:1:2");
		Object value = editor.getValue();
		assertTrimmedElements(value);
		assertThat(editor.getAsText()).isEqualTo("0:1:2");
	}

	@Test
	void withCharsToDelete() {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", "\r\n", false);
		editor.setAsText("0\r,1,\n2");
		Object value = editor.getValue();
		assertTrimmedElements(value);
		assertThat(editor.getAsText()).isEqualTo("0,1,2");
	}

	@Test
	void withEmptyArray() {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText("");
		Object value = editor.getValue();
		assertThat(value).isInstanceOf(String[].class);
		assertThat((String[]) value).isEmpty();
	}

	@Test
	void withEmptyArrayAsNull() {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", true);
		editor.setAsText("");
		assertThat(editor.getValue()).isNull();
	}

	private static void assertTrimmedElements(Object value) {
		assertThat(value).isInstanceOf(String[].class);
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertThat(array[i]).isEqualTo(("" + i));
		}
	}

}
