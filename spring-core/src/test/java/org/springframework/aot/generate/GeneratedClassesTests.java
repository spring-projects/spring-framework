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

import org.springframework.aot.generate.ClassGenerator.JavaFileGenerator;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GeneratedClasses}.
 *
 * @author Phillip Webb
 */
class GeneratedClassesTests {

	private GeneratedClasses generatedClasses = new GeneratedClasses(
			new ClassNameGenerator());

	private static final JavaFileGenerator JAVA_FILE_GENERATOR = GeneratedClassesTests::generateJavaFile;

	@Test
	void createWhenClassNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedClasses(null))
				.withMessage("'classNameGenerator' must not be null");
	}

	@Test
	void getOrGenerateWithClassTargetWhenJavaFileGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.getOrGenerateClass(null,
						TestTarget.class, "test"))
				.withMessage("'javaFileGenerator' must not be null");
	}

	@Test
	void getOrGenerateWithClassTargetWhenTargetIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses
						.getOrGenerateClass(JAVA_FILE_GENERATOR, (Class<?>) null, "test"))
				.withMessage("'target' must not be null");
	}

	@Test
	void getOrGenerateWithClassTargetWhenFeatureIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses
						.getOrGenerateClass(JAVA_FILE_GENERATOR, TestTarget.class, null))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void getOrGenerateWhenNewReturnsGeneratedMethod() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrGenerateClass(JAVA_FILE_GENERATOR, TestTarget.class, "one");
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrGenerateClass(JAVA_FILE_GENERATOR, TestTarget.class, "two");
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrGenerateWhenRepeatReturnsSameGeneratedMethod() {
		GeneratedClasses generated = this.generatedClasses;
		GeneratedClass generatedClass1 = generated.getOrGenerateClass(JAVA_FILE_GENERATOR,
				TestTarget.class, "one");
		GeneratedClass generatedClass2 = generated.getOrGenerateClass(JAVA_FILE_GENERATOR,
				TestTarget.class, "one");
		GeneratedClass generatedClass3 = generated.getOrGenerateClass(JAVA_FILE_GENERATOR,
				TestTarget.class, "one");
		GeneratedClass generatedClass4 = generated.getOrGenerateClass(JAVA_FILE_GENERATOR,
				TestTarget.class, "two");
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
				.isSameAs(generatedClass3).isNotSameAs(generatedClass4);
	}

	static JavaFile generateJavaFile(ClassName className,
			GeneratedMethods generatedMethods) {
		TypeSpec typeSpec = TypeSpec.classBuilder(className).addJavadoc("Test").build();
		return JavaFile.builder(className.packageName(), typeSpec).build();
	}

	private static class TestTarget {

	}

}
