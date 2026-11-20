/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
