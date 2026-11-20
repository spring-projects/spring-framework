/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.io.IOException;
import java.io.Reader;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ReaderEditor}.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
class ReaderEditorTests {

	@Test
	void ctorWithNullResourceEditor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ReaderEditor(null));
	}

	@Test
	void sunnyDay() throws IOException {
		Reader reader = null;
		try {
			String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
					"/" + ClassUtils.getShortName(getClass()) + ".class";
			ReaderEditor editor = new ReaderEditor();
			editor.setAsText(resource);
			Object value = editor.getValue();
			assertThat(value).isNotNull();
			assertThat(value).isInstanceOf(Reader.class);
			reader = (Reader) value;
			assertThat(reader.ready()).isTrue();
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test
	void whenResourceDoesNotExist() {
		String resource = "classpath:bingo!";
		ReaderEditor editor = new ReaderEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(resource));
	}

	@Test
	void getAsTextReturnsNullByDefault() {
		assertThat(new ReaderEditor().getAsText()).isNull();
		String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class";
		ReaderEditor editor = new ReaderEditor();
		editor.setAsText(resource);
		assertThat(editor.getAsText()).isNull();
	}

}
