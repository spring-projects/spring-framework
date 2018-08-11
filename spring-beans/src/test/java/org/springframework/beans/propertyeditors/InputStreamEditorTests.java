/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.InputStream;

import org.junit.Test;

import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * Unit tests for the {@link InputStreamEditor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class InputStreamEditorTests {

	@Test(expected = IllegalArgumentException.class)
	public void testCtorWithNullResourceEditor() throws Exception {
		new InputStreamEditor(null);
	}

	@Test
	public void testSunnyDay() throws Exception {
		InputStream stream = null;
		try {
			String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
					"/" + ClassUtils.getShortName(getClass()) + ".class";
			InputStreamEditor editor = new InputStreamEditor();
			editor.setAsText(resource);
			Object value = editor.getValue();
			assertNotNull(value);
			assertTrue(value instanceof InputStream);
			stream = (InputStream) value;
			assertTrue(stream.available() > 0);
		}
		finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWhenResourceDoesNotExist() throws Exception {
		InputStreamEditor editor = new InputStreamEditor();
		editor.setAsText("classpath:bingo!");
	}

	@Test
	public void testGetAsTextReturnsNullByDefault() throws Exception {
		assertNull(new InputStreamEditor().getAsText());
		String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) +
				"/" + ClassUtils.getShortName(getClass()) + ".class";
		InputStreamEditor editor = new InputStreamEditor();
		editor.setAsText(resource);
		assertNull(editor.getAsText());
	}

}
