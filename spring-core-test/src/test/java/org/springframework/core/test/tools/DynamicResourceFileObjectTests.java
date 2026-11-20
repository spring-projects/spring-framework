/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.tools.JavaFileObject.Kind;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

/**
 * Tests for {@link DynamicResourceFileObject}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @since 6.0
 */
class DynamicResourceFileObjectTests {

	@Test
	void getUriReturnsFileUri() {
		DynamicResourceFileObject fileObject = new DynamicResourceFileObject("META-INF/test.properties");
		assertThat(fileObject.toUri()).hasToString("resource:///META-INF/test.properties");
	}

	@Test
	void getKindReturnsOther() {
		DynamicResourceFileObject fileObject = new DynamicResourceFileObject("META-INF/test.properties");
		assertThat(fileObject.getKind()).isEqualTo(Kind.OTHER);
	}

	@Test
	void openOutputStreamWritesToBytes() throws Exception {
		DynamicResourceFileObject fileObject = new DynamicResourceFileObject("META-INF/test.properties");
		try(OutputStream outputStream = fileObject.openOutputStream()) {
			new ByteArrayInputStream("test".getBytes()).transferTo(outputStream);
		}
		assertThat(fileObject.getBytes()).isEqualTo("test".getBytes());
	}

	@Test
	void openInputStreamReadsFromBytes() throws Exception {
		DynamicResourceFileObject fileObject = new DynamicResourceFileObject("META-INF/test.properties");
		try(OutputStream outputStream = fileObject.openOutputStream()) {
			new ByteArrayInputStream("test".getBytes()).transferTo(outputStream);
		}
		try(InputStream inputStream = fileObject.openInputStream()) {
			assertThat(inputStream.readAllBytes()).isEqualTo("test".getBytes());
		}
	}

	@Test
	void openInputStreamWhenNothingWrittenThrowsException() {
		DynamicResourceFileObject fileObject = new DynamicResourceFileObject("META-INF/test.properties");
		assertThatIOException().isThrownBy(fileObject::openInputStream);
	}

}
