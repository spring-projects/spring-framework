/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void classpathFileName() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).exists();
	}

	@Test
	void withNonExistentResource() {
		PropertyEditor fileEditor = new FileEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				fileEditor.setAsText("classpath:no_way_this_file_is_found.doc"));
	}

	@Test
	void withNonExistentFile() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:no_way_this_file_is_found.doc");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).doesNotExist();
	}

	@Test
	void absoluteFileName() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("/no_way_this_file_is_found.doc");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).doesNotExist();
	}

	@Test
	void currentDirectory() {
		PropertyEditor fileEditor = new FileEditor();
		fileEditor.setAsText("file:.");
		Object value = fileEditor.getValue();
		assertThat(value).isInstanceOf(File.class);
		File file = (File) value;
		assertThat(file).isEqualTo(new File("."));
	}

	@Test
	void unqualifiedFileNameFound() {
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
	void unqualifiedFileNameNotFound() {
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
