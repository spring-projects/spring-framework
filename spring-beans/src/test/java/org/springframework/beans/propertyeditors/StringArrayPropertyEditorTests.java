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
 */
public class StringArrayPropertyEditorTests {

	@Test
	public void withDefaultSeparator() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText("0,1,2");
		Object value = editor.getValue();
		assertThat(value).isNotNull();
		boolean condition = value instanceof String[];
		assertThat(condition).isTrue();
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertThat(array[i]).isEqualTo(("" + i));
		}
		assertThat(editor.getAsText()).isEqualTo("0,1,2");
	}

	@Test
	public void trimByDefault() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText(" 0,1 , 2 ");
		Object value = editor.getValue();
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertThat(array[i]).isEqualTo(("" + i));
		}
		assertThat(editor.getAsText()).isEqualTo("0,1,2");
	}

	@Test
	public void noTrim() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",",false,false);
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
	public void withCustomSeparator() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(":");
		editor.setAsText("0:1:2");
		Object value = editor.getValue();
		boolean condition = value instanceof String[];
		assertThat(condition).isTrue();
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertThat(array[i]).isEqualTo(("" + i));
		}
		assertThat(editor.getAsText()).isEqualTo("0:1:2");
	}

	@Test
	public void withCharsToDelete() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", "\r\n", false);
		editor.setAsText("0\r,1,\n2");
		Object value = editor.getValue();
		boolean condition = value instanceof String[];
		assertThat(condition).isTrue();
		String[] array = (String[]) value;
		for (int i = 0; i < array.length; ++i) {
			assertThat(array[i]).isEqualTo(("" + i));
		}
		assertThat(editor.getAsText()).isEqualTo("0,1,2");
	}

	@Test
	public void withEmptyArray() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor();
		editor.setAsText("");
		Object value = editor.getValue();
		boolean condition = value instanceof String[];
		assertThat(condition).isTrue();
		assertThat(((String[]) value).length).isEqualTo(0);
	}

	@Test
	public void withEmptyArrayAsNull() throws Exception {
		StringArrayPropertyEditor editor = new StringArrayPropertyEditor(",", true);
		editor.setAsText("");
		assertThat(editor.getValue()).isNull();
	}

}
