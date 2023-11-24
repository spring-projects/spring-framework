/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.mail.javamail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Brian Hanafee
 * @author Sam Brannen
 * @since 09.07.2005
 */
public class InternetAddressEditorTests {

	private static final String EMPTY = "";
	private static final String SIMPLE = "nobody@nowhere.com";
	private static final String BAD = "(";

	private final InternetAddressEditor editor = new InternetAddressEditor();


	@Test
	public void uninitialized() {
		assertThat(editor.getAsText()).as("Uninitialized editor did not return empty value string").isEmpty();
	}

	@Test
	public void setNull() {
		editor.setAsText(null);
		assertThat(editor.getAsText()).as("Setting null did not result in empty value string").isEmpty();
	}

	@Test
	public void setEmpty() {
		editor.setAsText(EMPTY);
		assertThat(editor.getAsText()).as("Setting empty string did not result in empty value string").isEmpty();
	}

	@Test
	public void allWhitespace() {
		editor.setAsText(" ");
		assertThat(editor.getAsText()).as("All whitespace was not recognized").isEmpty();
	}

	@Test
	public void simpleGoodAddress() {
		editor.setAsText(SIMPLE);
		assertThat(editor.getAsText()).as("Simple email address failed").isEqualTo(SIMPLE);
	}

	@Test
	public void excessWhitespace() {
		editor.setAsText(" " + SIMPLE + " ");
		assertThat(editor.getAsText()).as("Whitespace was not stripped").isEqualTo(SIMPLE);
	}

	@Test
	public void simpleBadAddress() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(BAD));
	}

}
