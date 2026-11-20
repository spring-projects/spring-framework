/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;

import javax.tools.JavaFileObject.Kind;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DynamicClassFileObject}.
 *
 * @author Phillip Webb
 */
class DynamicClassFileObjectTests {

	@Test
	void getUriReturnsGeneratedUriBasedOnClassName() {
		DynamicClassFileObject fileObject = new DynamicClassFileObject("com.example.MyClass");
		assertThat(fileObject.toUri()).hasToString("class:///com/example/MyClass.class");
	}

	@Test
	void getKindReturnsClass() {
		DynamicClassFileObject fileObject = new DynamicClassFileObject("com.example.MyClass");
		assertThat(fileObject.getKind()).isEqualTo(Kind.CLASS);
	}

	@Test
	void openOutputStreamWritesToBytes() throws Exception {
		DynamicClassFileObject fileObject = new DynamicClassFileObject("com.example.MyClass");
		try(OutputStream outputStream = fileObject.openOutputStream()) {
			new ByteArrayInputStream("test".getBytes()).transferTo(outputStream);
		}
		assertThat(fileObject.getBytes()).isEqualTo("test".getBytes());
	}

}
