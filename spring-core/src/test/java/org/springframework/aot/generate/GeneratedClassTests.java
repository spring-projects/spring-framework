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

package org.springframework.aot.generate;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedClass}.
 *
 * @author Phillip Webb
 */
class GeneratedClassTests {

	@Test
	void getNameReturnsName() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(this::generateJavaFile, name);
		assertThat(generatedClass.getName()).isSameAs(name);
	}

	@Test
	void generateJavaFileSuppliesGeneratedMethods() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(this::generateJavaFile, name);
		MethodGenerator methodGenerator = generatedClass.getMethodGenerator();
		methodGenerator.generateMethod("test")
				.using(builder -> builder.addJavadoc("Test Method"));
		assertThat(generatedClass.generateJavaFile().toString()).contains("Test Method");
	}

	@Test
	void generateJavaFileWhenHasBadPackageThrowsException() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(
				this::generateBadPackageJavaFile, name);
		assertThatIllegalStateException()
				.isThrownBy(
						() -> assertThat(generatedClass.generateJavaFile().toString()))
				.withMessageContaining("should be in package");
	}

	@Test
	void generateJavaFileWhenHasBadNameThrowsException() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(this::generateBadNameJavaFile,
				name);
		assertThatIllegalStateException()
				.isThrownBy(
						() -> assertThat(generatedClass.generateJavaFile().toString()))
				.withMessageContaining("should be named");
	}

	private JavaFile generateJavaFile(ClassName className, GeneratedMethods methods) {
		TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
		methods.doWithMethodSpecs(classBuilder::addMethod);
		return JavaFile.builder(className.packageName(), classBuilder.build()).build();
	}

	private JavaFile generateBadPackageJavaFile(ClassName className,
			GeneratedMethods methods) {
		TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className);
		return JavaFile.builder("naughty", classBuilder.build()).build();
	}

	private JavaFile generateBadNameJavaFile(ClassName className,
			GeneratedMethods methods) {
		TypeSpec.Builder classBuilder = TypeSpec.classBuilder("Naughty");
		return JavaFile.builder(className.packageName(), classBuilder.build()).build();
	}

}
