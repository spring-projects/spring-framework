/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.propertyeditors;

import java.io.Reader;

import org.junit.Test;

import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link ReaderEditor} class.
 *
 * @author Juergen Hoeller
 * @since 4.2
 */
public class ReaderEditorTests {

	@Test(expected = IllegalArgumentException.class)
	public void testCtorWithNullResourceEditor() throws Exception {
		new InputStreamEditor(null);
	}

	@Test
	public void testSunnyDay() throws Exception {
		Reader reader = null;
		try {
			String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
					"/" + ClassUtils.getShortName(getClass()) + ".class";
			ReaderEditor editor = new ReaderEditor();
			editor.setAsText(resource);
			Object value = editor.getValue();
			assertNotNull(value);
			assertTrue(value instanceof Reader);
			reader = (Reader) value;
			assertTrue(reader.ready());
		}
		finally {
			if (reader != null) {
				reader.close();
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWhenResourceDoesNotExist() throws Exception {
		String resource = "classpath:bingo!";
		ReaderEditor editor = new ReaderEditor();
		editor.setAsText(resource);
	}

	@Test
	public void testGetAsTextReturnsNullByDefault() throws Exception {
		assertNull(new ReaderEditor().getAsText());
		String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class";
		ReaderEditor editor = new ReaderEditor();
		editor.setAsText(resource);
		assertNull(editor.getAsText());
	}

}
