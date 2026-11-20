/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
	void classpathPathName() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("classpath:" + ClassUtils.classPackageAsResourcePath(getClass()) + "/" +
				ClassUtils.getShortName(getClass()) + ".class");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).exists();
	}

	@Test
	void withNonExistentResource() {
		PropertyEditor pathEditor = new PathEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				pathEditor.setAsText("classpath:/no_way_this_file_is_found.doc"));
	}

	@Test
	void withNonExistentPath() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("file:/no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).doesNotExist();
	}

	@Test
	void absolutePath() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("/no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).doesNotExist();
	}

	@Test
	void windowsAbsolutePath() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("C:\\no_way_this_file_is_found.doc");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path.toFile()).doesNotExist();
	}

	@Test
	void windowsAbsoluteFilePath() {
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
	void currentDirectory() {
		PropertyEditor pathEditor = new PathEditor();
		pathEditor.setAsText("file:.");
		Object value = pathEditor.getValue();
		assertThat(value).isInstanceOf(Path.class);
		Path path = (Path) value;
		assertThat(path).isEqualTo(Paths.get("."));
	}

	@Test
	void unqualifiedPathNameFound() {
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
	void unqualifiedPathNameNotFound() {
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
