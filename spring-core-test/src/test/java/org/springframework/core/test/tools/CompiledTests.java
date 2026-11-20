/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.test.tools;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link Compiled}.
 *
 * @author Phillip Webb
 */
class CompiledTests {

	private static final String HELLO_WORLD = """
			package com.example;

			public class HelloWorld implements java.util.function.Supplier<String> {

				public String get() {
					return "Hello World!";
				}

			}
			""";

	private static final String HELLO_SPRING = """
			package com.example;

			public class HelloSpring implements java.util.function.Supplier<String> {

				public String get() {
					return "Hello Spring!"; // !!
				}

			}
			""";

	@Test
	void getSourceFileWhenSingleReturnsSourceFile() {
		SourceFile sourceFile = SourceFile.of(HELLO_WORLD);
		TestCompiler.forSystem().compile(sourceFile,
				compiled -> assertThat(compiled.getSourceFile()).isSameAs(sourceFile));
	}

	@Test
	void getSourceFileWhenMultipleThrowsException() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
				SourceFile.of(HELLO_SPRING));
		TestCompiler.forSystem().compile(sourceFiles,
				compiled -> assertThatIllegalStateException().isThrownBy(
						compiled::getSourceFile));
	}

	@Test
	void getSourceFileWhenNoneThrowsException() {
		TestCompiler.forSystem().compile(
				compiled -> assertThatIllegalStateException().isThrownBy(
						compiled::getSourceFile));
	}

	@Test
	void getSourceFilesReturnsSourceFiles() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
				SourceFile.of(HELLO_SPRING));
		TestCompiler.forSystem().compile(sourceFiles,
				compiled -> assertThat(compiled.getSourceFiles()).isEqualTo(sourceFiles));
	}

	@Test
	void getResourceFileWhenSingleReturnsSourceFile() {
		ResourceFile resourceFile = ResourceFile.of("META-INF/myfile", "test");
		TestCompiler.forSystem().withResources(resourceFile).compile(
				compiled -> assertThat(compiled.getResourceFile()).isSameAs(
						resourceFile));
	}

	@Test
	void getResourceFileWhenMultipleThrowsException() {
		ResourceFiles resourceFiles = ResourceFiles.of(
				ResourceFile.of("META-INF/myfile1", "test1"),
				ResourceFile.of("META-INF/myfile2", "test2"));
		TestCompiler.forSystem().withResources(resourceFiles).compile(
				compiled -> assertThatIllegalStateException().isThrownBy(compiled::getResourceFile));
	}

	@Test
	void getResourceFileWhenNoneThrowsException() {
		TestCompiler.forSystem().compile(
				compiled -> assertThatIllegalStateException().isThrownBy(compiled::getResourceFile));
	}

	@Test
	void getResourceFilesReturnsResourceFiles() {
		ResourceFiles resourceFiles = ResourceFiles.of(
				ResourceFile.of("META-INF/myfile1", "test1"),
				ResourceFile.of("META-INF/myfile2", "test2"));
		TestCompiler.forSystem().withResources(resourceFiles).compile(
				compiled -> assertThat(compiled.getResourceFiles()).isEqualTo(
						resourceFiles));
	}

	@Test
	void getInstanceWhenNoneMatchesThrowsException() {
		TestCompiler.forSystem().compile(SourceFile.of(HELLO_WORLD),
				compiled -> assertThatIllegalStateException().isThrownBy(
						() -> compiled.getInstance(Callable.class)));
	}

	@Test
	void getInstanceWhenMultipleMatchesThrowsException() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
				SourceFile.of(HELLO_SPRING));
		TestCompiler.forSystem().compile(sourceFiles,
				compiled -> assertThatIllegalStateException().isThrownBy(
						() -> compiled.getInstance(Supplier.class)));
	}

	@Test
	void getInstanceWhenNoDefaultConstructorThrowsException() {
		SourceFile sourceFile = SourceFile.of("""
				package com.example;

				public class HelloWorld implements java.util.function.Supplier<String> {

					public HelloWorld(String name) {
					}

					public String get() {
						return "Hello World!";
					}

				}
				""");
		TestCompiler.forSystem().compile(sourceFile,
				compiled -> assertThatIllegalStateException().isThrownBy(
						() -> compiled.getInstance(Supplier.class)));
	}

	@Test
	void getInstanceReturnsInstance() {
		TestCompiler.forSystem().compile(SourceFile.of(HELLO_WORLD),
				compiled -> assertThat(compiled.getInstance(Supplier.class)).isNotNull());
	}

	@Test
	void getInstanceByNameReturnsInstance() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
				SourceFile.of(HELLO_SPRING));
		TestCompiler.forSystem().compile(sourceFiles,
				compiled -> assertThat(compiled.getInstance(Supplier.class,
						"com.example.HelloWorld")).isNotNull());
	}

	@Test
	void getAllCompiledClassesReturnsCompiledClasses() {
		SourceFiles sourceFiles = SourceFiles.of(SourceFile.of(HELLO_WORLD),
				SourceFile.of(HELLO_SPRING));
		TestCompiler.forSystem().compile(sourceFiles, compiled -> {
			List<Class<?>> classes = compiled.getAllCompiledClasses();
			assertThat(classes.stream().map(Class::getName)).containsExactlyInAnyOrder(
					"com.example.HelloWorld", "com.example.HelloSpring");
		});
	}

}
