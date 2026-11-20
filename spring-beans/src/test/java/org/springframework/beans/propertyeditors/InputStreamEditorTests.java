/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link InputStreamEditor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class InputStreamEditorTests {

	@Test
	void ctorWithNullResourceEditor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new InputStreamEditor(null));
	}

	@Test
	void sunnyDay() throws IOException {
		InputStream stream = null;
		try {
			String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
					"/" + ClassUtils.getShortName(getClass()) + ".class";
			InputStreamEditor editor = new InputStreamEditor();
			editor.setAsText(resource);
			Object value = editor.getValue();
			assertThat(value).isNotNull();
			assertThat(value).isInstanceOf(InputStream.class);
			stream = (InputStream) value;
			assertThat(stream.available()).isGreaterThan(0);
		}
		finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	@Test
	void whenResourceDoesNotExist() {
		InputStreamEditor editor = new InputStreamEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText("classpath:bingo!"));
	}

	@Test
	void getAsTextReturnsNullByDefault() {
		assertThat(new InputStreamEditor().getAsText()).isNull();
		String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class";
		InputStreamEditor editor = new InputStreamEditor();
		editor.setAsText(resource);
		assertThat(editor.getAsText()).isNull();
	}

}
