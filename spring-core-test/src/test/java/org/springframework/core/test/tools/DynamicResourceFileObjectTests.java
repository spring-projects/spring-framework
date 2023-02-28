/*
 * Copyright 2002-2022 the original author or authors.
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
