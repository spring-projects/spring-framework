/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CharArrayPropertyEditor}.
 *
 * @author Rick Evans
 */
class CharArrayPropertyEditorTests {

	private final PropertyEditor charEditor = new CharArrayPropertyEditor();

	@Test
	void sunnyDaySetAsText() {
		final String text = "Hideous towns make me throw... up";
		charEditor.setAsText(text);

		Object value = charEditor.getValue();
		assertThat(value).isInstanceOf(char[].class);
		char[] chars = (char[]) value;
		for (int i = 0; i < text.length(); ++i) {
			assertThat(chars[i]).as("char[] differs at index '" + i + "'").isEqualTo(text.charAt(i));
		}
		assertThat(charEditor.getAsText()).isEqualTo(text);
	}

	@Test
	void getAsTextReturnsEmptyStringIfValueIsNull() {
		assertThat(charEditor.getAsText()).isEmpty();

		charEditor.setAsText(null);
		assertThat(charEditor.getAsText()).isEmpty();
	}

}
