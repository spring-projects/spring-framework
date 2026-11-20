/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ByteArrayPropertyEditor}.
 *
 * @author Rick Evans
 */
class ByteArrayPropertyEditorTests {

	private final PropertyEditor byteEditor = new ByteArrayPropertyEditor();

	@Test
	void sunnyDaySetAsText() {
		final String text = "Hideous towns make me throw... up";
		byteEditor.setAsText(text);

		Object value = byteEditor.getValue();
		assertThat(value).isInstanceOf(byte[].class);
		byte[] bytes = (byte[]) value;
		for (int i = 0; i < text.length(); ++i) {
			assertThat(bytes[i]).as("cyte[] differs at index '" + i + "'").isEqualTo((byte) text.charAt(i));
		}
		assertThat(byteEditor.getAsText()).isEqualTo(text);
	}

	@Test
	void getAsTextReturnsEmptyStringIfValueIsNull() {
		assertThat(byteEditor.getAsText()).isEmpty();

		byteEditor.setAsText(null);
		assertThat(byteEditor.getAsText()).isEmpty();
	}

}
