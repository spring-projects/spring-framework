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

package org.springframework.aot.test.generator.compile;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generator.file.ResourceFile;
import org.springframework.aot.test.generator.file.ResourceFiles;
import org.springframework.aot.test.generator.file.SourceFile;
import org.springframework.aot.test.generator.file.SourceFiles;
import org.springframework.aot.test.generator.file.WritableContent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link TestCompiler}.
 *
 * @author Phillip Webb
 */
class TestCompilerTests {

	private static final String HELLO_WORLD = """
			package com.example;

			import java.util.function.Supplier;

			@Deprecated
			public class Hello implements Supplier<String> {

				public String get() {
					return "Hello World!";
				}

			}
			""";

	private static final String HELLO_SPRING = """
			package com.example;

			import java.util.function.Supplier;

			public class Hello implements Supplier<String> {

				public String get() {
					return "Hello Spring!"; // !!
				}

			}
			""";

	private static final String HELLO_BAD = """
			package com.example;

			public class Hello implements Supplier<String> {

				public String get() {
					return "Missing Import!";
				}

			}
			""";

	@Test
	@SuppressWarnings("unchecked")
	void compileWhenHasDifferentClassesWithSameClassNameCompilesBoth() {
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_WORLD)).compile(
				compiled -> {
					Supplier<String> supplier = compiled.getInstance(Supplier.class,
							"com.example.Hello");
					assertThat(supplier.get()).isEqualTo("Hello World!");
				});
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_SPRING)).compile(
				compiled -> {
					Supplier<String> supplier = compiled.getInstance(Supplier.class,
							"com.example.Hello");
					assertThat(supplier.get()).isEqualTo("Hello Spring!");
				});
	}

	@Test
	void compileAndGetSourceFile() {
		TestCompiler.forSystem().withSources(SourceFile.of(HELLO_SPRING)).compile(
				compiled -> assertThat(compiled.getSourceFile()).hasMethodNamed(
						"get").withBodyContaining("// !!"));
	}

	@Test
	void compileWhenSourceHasCompileErrors() {
		assertThatExceptionOfType(CompilationException.class).isThrownBy(
				() -> TestCompiler.forSystem().withSources(
						SourceFile.of(HELLO_BAD)).compile(compiled -> {
						}));
	}

	@Test
	void withSourcesArrayAddsSource() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		TestCompiler.forSystem().withSources(sourceFile).compile(
				this::assertSuppliesHelloWorld);
	}

	@Test
	void withSourcesAddsSource() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD));
		TestCompiler.forSystem().withSources(sourceFiles).compile(
				this::assertSuppliesHelloWorld);
	}

	@Test
	void withResourcesArrayAddsResource() {
		ResourceFile resourceFile = ResourceFile.of("META-INF/myfile", "test");
		TestCompiler.forSystem().withResources(resourceFile).compile(
				this::assertHasResource);
	}

	@Test
	void withResourcesAddsResource() {
		ResourceFiles resourceFiles = ResourceFiles.of(
				ResourceFile.of("META-INF/myfile", "test"));
		TestCompiler.forSystem().withResources(resourceFiles).compile(
				this::assertHasResource);
	}

	@Test
	void compileWithWritableContent() {
		WritableContent content = appendable -> appendable.append(HELLO_WORLD);
		TestCompiler.forSystem().compile(content, this::assertSuppliesHelloWorld);
	}

	@Test
	void compileWithSourceFile() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		TestCompiler.forSystem().compile(sourceFile, this::assertSuppliesHelloWorld);
	}

	@Test
	void compileWithSourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD));
		TestCompiler.forSystem().compile(sourceFiles, this::assertSuppliesHelloWorld);
	}

	@Test
	void compileWithSourceFilesAndResourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD));
		ResourceFiles resourceFiles = ResourceFiles.of(
				ResourceFile.of("META-INF/myfile", "test"));
		TestCompiler.forSystem().compile(sourceFiles, resourceFiles, compiled -> {
			assertSuppliesHelloWorld(compiled);
			assertHasResource(compiled);
		});
	}

	private void assertSuppliesHelloWorld(Compiled compiled) {
		assertThat(compiled.getInstance(Supplier.class).get()).isEqualTo("Hello World!");
	}

	private void assertHasResource(Compiled compiled) {
		assertThat(compiled.getClassLoader().getResourceAsStream(
				"META-INF/myfile")).hasContent("test");
	}

}
