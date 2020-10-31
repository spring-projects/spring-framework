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

import java.beans.PropertyEditor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link ByteArrayPropertyEditor} class.
 *
 * @author Rick Evans
 */
public class ByteArrayPropertyEditorTests {

	private final PropertyEditor byteEditor = new ByteArrayPropertyEditor();

	@Test
	public void sunnyDaySetAsText() throws Exception {
		final String text = "Hideous towns make me throw... up";
		byteEditor.setAsText(text);

		Object value = byteEditor.getValue();
		assertThat(value).isNotNull().isInstanceOf(byte[].class);
		byte[] bytes = (byte[]) value;
		for (int i = 0; i < text.length(); ++i) {
			assertThat(bytes[i]).as("cyte[] differs at index '" + i + "'").isEqualTo((byte) text.charAt(i));
		}
		assertThat(byteEditor.getAsText()).isEqualTo(text);
	}

	@Test
	public void getAsTextReturnsEmptyStringIfValueIsNull() throws Exception {
		assertThat(byteEditor.getAsText()).isEqualTo("");

		byteEditor.setAsText(null);
		assertThat(byteEditor.getAsText()).isEqualTo("");
	}

}
