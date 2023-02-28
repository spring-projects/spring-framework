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

import javax.lang.model.element.Modifier;

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

	private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

	private static final Consumer<TypeSpec.Builder> emptyTypeCustomizer = type -> {};

	private static final Consumer<MethodSpec.Builder> emptyMethodCustomizer = method -> {};

	@Test
	void getEnclosingNameOnTopLevelClassReturnsNull() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		assertThat(generatedClass.getEnclosingClass()).isNull();
	}

	@Test
	void getEnclosingNameOnInnerClassReturnsParent() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		GeneratedClass innerGeneratedClass = generatedClass.getOrAdd("Test", emptyTypeCustomizer);
		assertThat(innerGeneratedClass.getEnclosingClass()).isEqualTo(generatedClass);
	}

	@Test
	void getNameReturnsName() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		assertThat(generatedClass.getName()).isSameAs(TEST_CLASS_NAME);
	}

	@Test
	void reserveMethodNamesWhenNameUsedThrowsException() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		generatedClass.getMethods().add("apply", emptyMethodCustomizer);
		assertThatIllegalStateException()
				.isThrownBy(() -> generatedClass.reserveMethodNames("apply"));
	}

	@Test
	void reserveMethodNamesReservesNames() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		generatedClass.reserveMethodNames("apply");
		GeneratedMethod generatedMethod = generatedClass.getMethods().add("apply", emptyMethodCustomizer);
		assertThat(generatedMethod.getName()).isEqualTo("apply1");
	}

	@Test
	void generateMethodNameWhenAllEmptyPartsGeneratesSetName() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		GeneratedMethod generatedMethod = generatedClass.getMethods().add("123", emptyMethodCustomizer);
		assertThat(generatedMethod.getName()).isEqualTo("$$aot");
	}

	@Test
	void getOrAddWhenRepeatReturnsSameGeneratedClass() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		GeneratedClass innerGeneratedClass = generatedClass.getOrAdd("Inner", emptyTypeCustomizer);
		GeneratedClass innerGeneratedClass2 = generatedClass.getOrAdd("Inner", emptyTypeCustomizer);
		GeneratedClass innerGeneratedClass3 = generatedClass.getOrAdd("Inner", emptyTypeCustomizer);
		assertThat(innerGeneratedClass).isSameAs(innerGeneratedClass2).isSameAs(innerGeneratedClass3);
	}

	@Test
	void generateJavaFileIncludesGeneratedMethods() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		generatedClass.getMethods().add("test", method -> method.addJavadoc("Test Method"));
		assertThat(generatedClass.generateJavaFile().toString()).contains("Test Method");
	}

	@Test
	void generateJavaFileIncludesDeclaredClasses() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME);
		generatedClass.getOrAdd("First", type -> type.modifiers.add(Modifier.STATIC));
		generatedClass.getOrAdd("Second", type -> type.modifiers.add(Modifier.PRIVATE));
		assertThat(generatedClass.generateJavaFile().toString())
				.contains("static class First").contains("private class Second");
	}

	@Test
	void generateJavaFileOnInnerClassThrowsException() {
		GeneratedClass generatedClass = createGeneratedClass(TEST_CLASS_NAME)
				.getOrAdd("Inner", emptyTypeCustomizer);
		assertThatIllegalStateException().isThrownBy(generatedClass::generateJavaFile);
	}

	private static GeneratedClass createGeneratedClass(ClassName className) {
		return new GeneratedClass(className, emptyTypeCustomizer);
	}

}
