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

import org.junit.jupiter.api.Test;

import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Thomas Risberg
 * @author Chris Beams
 * @author Juergen Hoeller
 */
class FileEditorTests {

	@Test
	void testClasspathFileName() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).exists();
	}

	@Test
	void testWithNonExistentResource() {
		PropertyEditor fileEditor = new FileEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				fileEditor.setAsText("classpath:no_way_this_file_is_found.doc"));
	}

	@Test
	void testWithNonExistentFile() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:no_way_this_file_is_found.doc");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).doesNotExist();
	}

	@Test
	void testAbsoluteFileName() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("/no_way_this_file_is_found.doc");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).doesNotExist();
	}

	@Test
	void testCurrentDirectory() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:.");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).isEqualTo(new File("."));
	}

	@Test
	void testUnqualifiedFileNameFound() {
		PropertyEditor fileEditor = new FileEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class";
		fileEditor.setAsText(fileName);
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).exists();
		String absolutePath = file.getAbsolutePath().replace('\\', '/');
		assertThat(absolutePath).endsWith(fileName);
	}

	@Test
	void testUnqualifiedFileNameNotFound() {
		PropertyEditor fileEditor = new FileEditor();
		String fileName = ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".clazz";
		fileEditor.setAsText(fileName);
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).doesNotExist();
		String absolutePath = file.getAbsolutePath().replace('\\', '/');
		assertThat(absolutePath).endsWith(fileName);
	}

}
