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

import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedMethod}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedMethodTests {

	private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

	private static final Consumer<MethodSpec.Builder> emptyMethod = method -> {};

	private static final String NAME = "spring";

	@Test
	void getNameReturnsName() {
		GeneratedMethod generatedMethod = new GeneratedMethod(TEST_CLASS_NAME, NAME, emptyMethod);
		assertThat(generatedMethod.getName()).isSameAs(NAME);
	}

	@Test
	void generateMethodSpecReturnsMethodSpec() {
		GeneratedMethod generatedMethod = create(method -> method.addJavadoc("Test"));
		assertThat(generatedMethod.getMethodSpec().javadoc).asString().contains("Test");
	}

	@Test
	void generateMethodSpecWhenMethodNameIsChangedThrowsException() {
		assertThatIllegalStateException().isThrownBy(() ->
						create(method -> method.setName("badname")).getMethodSpec())
				.withMessage("'method' consumer must not change the generated method name");
	}

	@Test
	void toMethodReferenceWithInstanceMethod() {
		GeneratedMethod generatedMethod = create(emptyMethod);
		MethodReference methodReference = generatedMethod.toMethodReference();
		assertThat(methodReference).isNotNull();
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME))
				.isEqualTo(CodeBlock.of("spring()"));
	}

	@Test
	void toMethodReferenceWithStaticMethod() {
		GeneratedMethod generatedMethod = create(method -> method.addModifiers(Modifier.STATIC));
		MethodReference methodReference = generatedMethod.toMethodReference();
		assertThat(methodReference).isNotNull();
		ClassName anotherDeclaringClass = ClassName.get("com.example", "Another");
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), anotherDeclaringClass))
				.isEqualTo(CodeBlock.of("com.example.Test.spring()"));
	}

	private GeneratedMethod create(Consumer<MethodSpec.Builder> method) {
		return new GeneratedMethod(TEST_CLASS_NAME, NAME, method);
	}

}
