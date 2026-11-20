/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart.support;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Rick Evans
 * @author Sam Brannen
 */
class ByteArrayMultipartFileEditorTests {

	private final ByteArrayMultipartFileEditor editor = new ByteArrayMultipartFileEditor();

	@Test
	void setValueAsByteArray() {
		String expectedValue = "Shumwere, shumhow, a shuck ish washing you. - Drunken Far Side";
		editor.setValue(expectedValue.getBytes());
		assertThat(editor.getAsText()).isEqualTo(expectedValue);
	}

	@Test
	void setValueAsString() {
		String expectedValue = "'Green Wing' - classic British comedy";
		editor.setValue(expectedValue);
		assertThat(editor.getAsText()).isEqualTo(expectedValue);
	}

	@Test
	void setValueAsCustomObjectInvokesToString() {
		final String expectedValue = "'Green Wing' - classic British comedy";
		Object object = new Object() {
			@Override
			public String toString() {
				return expectedValue;
			}
		};

		editor.setValue(object);
		assertThat(editor.getAsText()).isEqualTo(expectedValue);
	}

	@Test
	void setValueAsNullGetsBackEmptyString() {
		editor.setValue(null);
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void setValueAsMultipartFile() throws Exception {
		String expectedValue = "That is comforting to know";
		MultipartFile file = mock();
		given(file.getBytes()).willReturn(expectedValue.getBytes());
		editor.setValue(file);
		assertThat(editor.getAsText()).isEqualTo(expectedValue);
	}

	@Test
	void setValueAsMultipartFileWithBadBytes() throws Exception {
		MultipartFile file = mock();
		given(file.getBytes()).willThrow(new IOException());
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setValue(file));
	}

}
