/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
class InternetAddressEditorTests {

	private static final String EMPTY = "";
	private static final String SIMPLE = "nobody@nowhere.com";
	private static final String BAD = "(";

	private final InternetAddressEditor editor = new InternetAddressEditor();


	@Test
	void uninitialized() {
		assertThat(editor.getAsText()).as("Uninitialized editor did not return empty value string").isEmpty();
	}

	@Test
	void setNull() {
		editor.setAsText(null);
		assertThat(editor.getAsText()).as("Setting null did not result in empty value string").isEmpty();
	}

	@Test
	void setEmpty() {
		editor.setAsText(EMPTY);
		assertThat(editor.getAsText()).as("Setting empty string did not result in empty value string").isEmpty();
	}

	@Test
	void allWhitespace() {
		editor.setAsText(" ");
		assertThat(editor.getAsText()).as("All whitespace was not recognized").isEmpty();
	}

	@Test
	void simpleGoodAddress() {
		editor.setAsText(SIMPLE);
		assertThat(editor.getAsText()).as("Simple email address failed").isEqualTo(SIMPLE);
	}

	@Test
	void excessWhitespace() {
		editor.setAsText(" " + SIMPLE + " ");
		assertThat(editor.getAsText()).as("Whitespace was not stripped").isEqualTo(SIMPLE);
	}

	@Test
	void simpleBadAddress() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(BAD));
	}

}
