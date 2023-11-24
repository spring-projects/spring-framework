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
