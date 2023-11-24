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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link SourceFile}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class SourceFileTests {

	private static final String HELLO_WORLD = """
			package com.example.helloworld;

			public class HelloWorld {
				public static void main(String[] args) {
					System.out.println("Hello World!");
				}
			}
			""";

	@Test
	void ofWhenContentIsEmptyThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of("")).withMessage(
				"WritableContent did not append any content");
	}

	@Test
	void ofWhenSourceDefinesNoClassThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> SourceFile.of("package com.example;")).withMessageContaining(
						"Unable to parse").havingCause().withMessage(
								"Source must define a single class");
	}

	@Test
	void ofWhenSourceDefinesMultipleClassesThrowsException() {
		assertThatIllegalStateException().isThrownBy(() -> SourceFile.of(
				"public class One {}\npublic class Two{}")).withMessageContaining(
						"Unable to parse").havingCause().withMessage(
								"Source must define a single class");
	}

	@Test
	void ofWhenSourceCannotBeParsedThrowsException() {
		assertThatIllegalStateException().isThrownBy(
				() -> SourceFile.of("well this is broken {")).withMessageContaining(
						"Unable to parse source file content");
	}

	@Test
	void ofWithoutPathDeducesPath() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.getPath()).isEqualTo(
				"com/example/helloworld/HelloWorld.java");
	}

	@Test
	void ofWithPathUsesPath() {
		SourceFile sourceFile = SourceFile.of("com/example/DifferentPath.java",
				HELLO_WORLD);
		assertThat(sourceFile.getPath()).isEqualTo("com/example/DifferentPath.java");
	}

	@Test
	void forClassWithClassUsesClass() {
		SourceFile sourceFile = SourceFile.forClass(new File("src/test/java"), SourceFileTests.class);
		assertThat(sourceFile.getPath()).isEqualTo("org/springframework/core/test/tools/SourceFileTests.java");
		assertThat(sourceFile.getClassName()).isEqualTo("org.springframework.core.test.tools.SourceFileTests");
	}

	@Test
	void forTestClassWithClassUsesClass() {
		SourceFile sourceFile = SourceFile.forTestClass(SourceFileTests.class);
		assertThat(sourceFile.getPath()).isEqualTo("org/springframework/core/test/tools/SourceFileTests.java");
		assertThat(sourceFile.getClassName()).isEqualTo("org.springframework.core.test.tools.SourceFileTests");
	}

	@Test
	void getContentReturnsContent() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.getContent()).isEqualTo(HELLO_WORLD);
	}

	@Test
	@SuppressWarnings("deprecation")
	void assertThatReturnsAssert() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		assertThat(sourceFile.assertThat()).isInstanceOf(SourceFileAssert.class);
	}

	@Test
	void createFromJavaPoetStyleApi() {
		JavaFile javaFile = new JavaFile(HELLO_WORLD);
		SourceFile sourceFile = SourceFile.of(javaFile::writeTo);
		assertThat(sourceFile.getContent()).isEqualTo(HELLO_WORLD);
	}

	@Test
	void getClassNameFromSimpleRecord() {
		SourceFile sourceFile = SourceFile.of("""
				package com.example.helloworld;

				record HelloWorld(String name) {
				}
				""");
		assertThat(sourceFile.getClassName()).isEqualTo("com.example.helloworld.HelloWorld");
	}

	@Test
	void getClassNameFromMoreComplexRecord() {
		SourceFile sourceFile = SourceFile.of("""
				package com.example.helloworld;

				public record HelloWorld(String name) {

					String getFoo() {
						return name();
					}

				}
				""");
		assertThat(sourceFile.getClassName()).isEqualTo("com.example.helloworld.HelloWorld");
	}

	@Test
	void getClassNameFromAnnotatedRecord() {
		SourceFile sourceFile = SourceFile.of("""
			package com.example;

			public record RecordProperties(
					@org.springframework.lang.NonNull("test") String property1,
					@org.springframework.lang.NonNull("test") String property2) {
			}
		""");
		assertThat(sourceFile.getClassName()).isEqualTo("com.example.RecordProperties");
	}

	/**
	 * JavaPoet style API with a {@code writeTo} method.
	 */
	static class JavaFile {

		private final String content;

		JavaFile(String content) {
			this.content = content;
		}

		void writeTo(Appendable out) throws IOException {
			out.append(this.content);
		}

	}

}
