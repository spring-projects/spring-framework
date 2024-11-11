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

import java.beans.PropertyEditor;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Juergen Hoeller
 * @since 4.3.2
 */
class PathEditorTests {

	@Test
	void testClasspathPathName() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).exists();
	}

	@Test
	void testWithNonExistentResource() {
		PropertyEditor pathEditor = new PathEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				pathEditor.setAsText("classpath:/no_way_this_file_is_found.doc"));
	}

	@Test
	void testWithNonExistentPath() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("file:/no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).doesNotExist();
	}

	@Test
	void testAbsolutePath() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("/no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).doesNotExist();
	}

	@Test
	void testWindowsAbsolutePath() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("C:\\no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).doesNotExist();
	}

	@Test
	void testWindowsAbsoluteFilePath() {
		PropertyEditor pathEditor = new PathEditor();
		try {
			pathEditor.setAsText("file://C:\\no_way_this_file_is_found.doc");
			Object value = pathEditor.getValue();
			assertThat(value).isInstanceOf(Path.class);
			Path path = (Path) value;
			assertThat(path.toFile()).doesNotExist();
		}
		catch (IllegalArgumentException ex) {
			if (File.separatorChar == '\\') {  // on Windows, otherwise silently ignore
				throw ex;
			}
		}
	}

	@Test
	void testCurrentDirectory() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("file:.");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path).isEqualTo(Paths.get("."));
	}

	@Test
	void testUnqualifiedPathNameFound() {
		PropertyEditor pathEditor = new PathEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class";
		pathEditor.setAsText(fileName);
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		File file = path.toFile();
		assertThat(file).exists();
		String absolutePath = file.getAbsolutePath();
		if (File.separatorChar == '\\') {
			absolutePath = absolutePath.replace('\\', '/');
		}
		assertThat(absolutePath).endsWith(fileName);
	}

	@Test
	void testUnqualifiedPathNameNotFound() {
		PropertyEditor pathEditor = new PathEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".clazz";
		pathEditor.setAsText(fileName);
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		File file = path.toFile();
		assertThat(file).doesNotExist();
		String absolutePath = file.getAbsolutePath();
		if (File.separatorChar == '\\') {
			absolutePath = absolutePath.replace('\\', '/');
		}
		assertThat(absolutePath).endsWith(fileName);
	}

}
