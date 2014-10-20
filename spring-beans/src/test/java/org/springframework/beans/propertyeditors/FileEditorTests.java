/*
 * Copyright 2002-2008 the original author or authors.
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

import java.beans.PropertyEditor;
import java.io.File;

import org.junit.Test;

import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * @author Thomas Risberg
 * @author Chris Beams
 */
public final class FileEditorTests {

	@Test
	public void testClasspathFileName() throws Exception {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/"
				+ ClassUtils.getShortName(getClass()) + ".class");
		Object value = fileEditor.getValue();
		assertTrue(value instanceof File);
		File file = (File) value;
		assertTrue(file.exists());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNonExistentResource() throws Exception {
		PropertyEditor propertyEditor = new FileEditor();
		propertyEditor.setAsText("classpath:no_way_this_file_is_found.doc");
	}

	@Test
	public void testWithNonExistentFile() throws Exception {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:no_way_this_file_is_found.doc");
		Object value = fileEditor.getValue();
		assertTrue(value instanceof File);
		File file = (File) value;
		assertTrue(!file.exists());
	}

	@Test
	public void testAbsoluteFileName() throws Exception {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("/no_way_this_file_is_found.doc");
		Object value = fileEditor.getValue();
		assertTrue(value instanceof File);
		File file = (File) value;
		assertTrue(!file.exists());
	}

	@Test
	public void testUnqualifiedFileNameFound() throws Exception {
		PropertyEditor fileEditor = new FileEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" + ClassUtils.getShortName(getClass())
				+ ".class";
		fileEditor.setAsText(fileName);
		Object value = fileEditor.getValue();
		assertTrue(value instanceof File);
		File file = (File) value;
		assertTrue(file.exists());
		String absolutePath = file.getAbsolutePath();
		if (File.separatorChar == '\\') {
			absolutePath = absolutePath.replace('\\', '/');
		}
		assertTrue(absolutePath.endsWith(fileName));
	}

	@Test
	public void testUnqualifiedFileNameNotFound() throws Exception {
		PropertyEditor fileEditor = new FileEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" + ClassUtils.getShortName(getClass())
				+ ".clazz";
		fileEditor.setAsText(fileName);
		Object value = fileEditor.getValue();
		assertTrue(value instanceof File);
		File file = (File) value;
		assertFalse(file.exists());
		String absolutePath = file.getAbsolutePath();
		if (File.separatorChar == '\\') {
			absolutePath = absolutePath.replace('\\', '/');
		}
		assertTrue(absolutePath.endsWith(fileName));
	}
}
