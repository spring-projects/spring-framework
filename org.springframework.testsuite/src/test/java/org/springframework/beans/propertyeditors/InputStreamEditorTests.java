/*
 * Copyright 2002-2006 the original author or authors.
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

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;
import org.springframework.util.ClassUtils;

import java.io.InputStream;

/**
 * Unit tests for the {@link InputStreamEditor} class.
 *
 * @author Rick Evans
 */
public final class InputStreamEditorTests extends TestCase {

	public void testCtorWithNullResourceEditor() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				new InputStreamEditor(null);
			}
		}.runTest();
	}

	public void testSunnyDay() throws Exception {
		InputStream stream = null;
		try {
			String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" + ClassUtils.getShortName(getClass()) + ".class";
			InputStreamEditor editor = new InputStreamEditor();
			editor.setAsText(resource);
			Object value = editor.getValue();
			assertNotNull(value);
			assertTrue(value instanceof InputStream);
			stream = (InputStream) value;
			assertTrue(stream.available() > 0);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}
	}

	public void testWhenResourceDoesNotExist() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				String resource = "classpath:bingo!";
				InputStreamEditor editor = new InputStreamEditor();
				editor.setAsText(resource);
			}
		}.runTest();
	}

	public void testGetAsTextReturnsNullByDefault() throws Exception {
		assertNull(new InputStreamEditor().getAsText());
		String resource = "classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" + ClassUtils.getShortName(getClass()) + ".class";
		InputStreamEditor editor = new InputStreamEditor();
		editor.setAsText(resource);
		assertNull(editor.getAsText());
	}

}
