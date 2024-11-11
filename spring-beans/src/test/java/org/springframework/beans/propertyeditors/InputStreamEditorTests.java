/*
 * Copyright 2002-2024 the original author or authors.
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
	void testCtorWithNullResourceEditor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new InputStreamEditor(null));
	}

	@Test
	void testSunnyDay() throws IOException {
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
	void testWhenResourceDoesNotExist() {
		InputStreamEditor editor = new InputStreamEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText("classpath:bingo!"));
	}

	@Test
	void testGetAsTextReturnsNullByDefault() {
		assertThat(new InputStreamEditor().getAsText()).isNull();
		String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class";
		InputStreamEditor editor = new InputStreamEditor();
		editor.setAsText(resource);
		assertThat(editor.getAsText()).isNull();
	}

}
