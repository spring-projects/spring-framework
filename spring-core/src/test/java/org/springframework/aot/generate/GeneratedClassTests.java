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

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedClass}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedClassTests {

	private static final Consumer<TypeSpec.Builder> emptyTypeCustomizer = type -> {};

	private static final Consumer<MethodSpec.Builder> emptyMethodCustomizer = method -> {};

	@Test
	void getNameReturnsName() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(name, emptyTypeCustomizer);
		assertThat(generatedClass.getName()).isSameAs(name);
	}

	@Test
	void reserveMethodNamesWhenNameUsedThrowsException() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(name, emptyTypeCustomizer);
		generatedClass.getMethods().add("apply", emptyMethodCustomizer);
		assertThatIllegalStateException()
				.isThrownBy(() -> generatedClass.reserveMethodNames("apply"));
	}

	@Test
	void reserveMethodNamesReservesNames() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(name, emptyTypeCustomizer);
		generatedClass.reserveMethodNames("apply");
		GeneratedMethod generatedMethod = generatedClass.getMethods().add("apply", emptyMethodCustomizer);
		assertThat(generatedMethod.getName()).isEqualTo("apply1");
	}

	@Test
	void generateMethodNameWhenAllEmptyPartsGeneratesSetName() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(name, emptyTypeCustomizer);
		GeneratedMethod generatedMethod = generatedClass.getMethods().add("123", emptyMethodCustomizer);
		assertThat(generatedMethod.getName()).isEqualTo("$$aot");
	}

	@Test
	void generateJavaFileIncludesGeneratedMethods() {
		ClassName name = ClassName.bestGuess("com.example.Test");
		GeneratedClass generatedClass = new GeneratedClass(name, emptyTypeCustomizer);
		generatedClass.getMethods().add("test", method -> method.addJavadoc("Test Method"));
		assertThat(generatedClass.generateJavaFile().toString()).contains("Test Method");
	}

}
