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

package org.springframework.beans.propertyeditors;

import java.io.IOException;
import java.io.Reader;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link ReaderEditor} class.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class ReaderEditorTests {

	@Test
	public void testCtorWithNullResourceEditor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ReaderEditor(null));
	}

	@Test
	public void testSunnyDay() throws IOException {
		Reader reader = null;
		try {
			String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
					"/" + ClassUtils.getShortName(getClass()) + ".class";
			ReaderEditor editor = new ReaderEditor();
			editor.setAsText(resource);
			Object value = editor.getValue();
			assertThat(value).isNotNull();
			assertThat(value instanceof Reader).isTrue();
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
	public void testWhenResourceDoesNotExist() {
		String resource = "classpath:bingo!";
		ReaderEditor editor = new ReaderEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(resource));
	}

	@Test
	public void testGetAsTextReturnsNullByDefault() {
		assertThat(new ReaderEditor().getAsText()).isNull();
		String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class";
		ReaderEditor editor = new ReaderEditor();
		editor.setAsText(resource);
		assertThat(editor.getAsText()).isNull();
	}

}
