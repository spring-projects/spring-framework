/*
 * Copyright 2002-2016 the original author or authors.
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

import java.beans.PropertyEditor;
import java.io.File;
import java.nio.file.Path;

import org.junit.Test;

import org.springframework.util.ClassUtils;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 4.3.2
 */
public class PathEditorTests {

	@Test
	public void testClasspathPathName() throws Exception {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class");
		Object value = pathEditor.getValue();
		assertTrue(value instanceof Path);
		Path path = (Path) value;
		assertTrue(path.toFile().exists());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWithNonExistentResource() throws Exception {
		PropertyEditor propertyEditor = new PathEditor();
		propertyEditor.setAsText("classpath:/no_way_this_file_is_found.doc");
	}

	@Test
	public void testWithNonExistentPath() throws Exception {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("file:/no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertTrue(value instanceof Path);
		Path path = (Path) value;
		assertTrue(!path.toFile().exists());
	}

	@Test
	public void testAbsolutePath() throws Exception {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("/no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertTrue(value instanceof Path);
		Path path = (Path) value;
		assertTrue(!path.toFile().exists());
	}

	@Test
	public void testUnqualifiedPathNameFound() throws Exception {
		PropertyEditor pathEditor = new PathEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class";
		pathEditor.setAsText(fileName);
		Object value = pathEditor.getValue();
		assertTrue(value instanceof Path);
		Path path = (Path) value;
		File file = path.toFile();
		assertTrue(file.exists());
		String absolutePath = file.getAbsolutePath();
		if (File.separatorChar == '\\') {
			absolutePath = absolutePath.replace('\\', '/');
		}
		assertTrue(absolutePath.endsWith(fileName));
	}

	@Test
	public void testUnqualifiedPathNameNotFound() throws Exception {
		PropertyEditor pathEditor = new PathEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".clazz";
		pathEditor.setAsText(fileName);
		Object value = pathEditor.getValue();
		assertTrue(value instanceof Path);
		Path path = (Path) value;
		File file = path.toFile();
		assertFalse(file.exists());
		String absolutePath = file.getAbsolutePath();
		if (File.separatorChar == '\\') {
			absolutePath = absolutePath.replace('\\', '/');
		}
		assertTrue(absolutePath.endsWith(fileName));
	}

}
