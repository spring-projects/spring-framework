/*
 * Copyright 2002-2024 the original author or authors.
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

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.MethodReference.ArgumentCodeGenerator;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.javapoet.TypeName;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link DefaultMethodReference}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class DefaultMethodReferenceTests {

	private static final String EXPECTED_STATIC = "org.springframework.aot.generate.DefaultMethodReferenceTests::someMethod";

	private static final String EXPECTED_ANONYMOUS_INSTANCE = "<instance>::someMethod";

	private static final String EXPECTED_DECLARED_INSTANCE = "<org.springframework.aot.generate.DefaultMethodReferenceTests>::someMethod";

	private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

	private static final ClassName INITIALIZER_CLASS_NAME = ClassName.get("com.example", "Initializer");

	@Test
	void createWithStringCreatesMethodReference() {
		MethodSpec method = createTestMethod("someMethod", new TypeName[0]);
		MethodReference reference = new DefaultMethodReference(method, null);
		assertThat(reference).hasToString(EXPECTED_ANONYMOUS_INSTANCE);
	}

	@Test
	void createWithClassNameAndStringCreateMethodReference() {
		ClassName declaringClass = ClassName.get(DefaultMethodReferenceTests.class);
		MethodReference reference = createMethodReference("someMethod", new TypeName[0], declaringClass);
		assertThat(reference).hasToString(EXPECTED_DECLARED_INSTANCE);
	}

	@Test
	void createWithStaticAndClassAndStringCreatesMethodReference() {
		ClassName declaringClass = ClassName.get(DefaultMethodReferenceTests.class);
		MethodReference reference = createStaticMethodReference("someMethod", declaringClass);
		assertThat(reference).hasToString(EXPECTED_STATIC);
	}

	@Test
	void toCodeBlock() {
		assertThat(createLocalMethodReference("methodName").toCodeBlock())
				.isEqualTo(CodeBlock.of("this::methodName"));
	}

	@Test
	void toCodeBlockWithStaticMethod() {
		assertThat(createStaticMethodReference("methodName", TEST_CLASS_NAME).toCodeBlock())
				.isEqualTo(CodeBlock.of("com.example.Test::methodName"));
	}

	@Test
	void toCodeBlockWithStaticMethodRequiresDeclaringClass() {
		MethodSpec method = createTestMethod("methodName", new TypeName[0], Modifier.STATIC);
		MethodReference methodReference = new DefaultMethodReference(method, null);
		assertThatIllegalStateException().isThrownBy(methodReference::toCodeBlock)
				.withMessage("Static method reference must define a declaring class");
	}

	@Test
	void toInvokeCodeBlockWithNullDeclaringClassAndTargetClass() {
		MethodSpec method = createTestMethod("methodName", new TypeName[0]);
		MethodReference methodReference = new DefaultMethodReference(method, null);
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME))
				.isEqualTo(CodeBlock.of("methodName()"));
	}

	@Test
	void toInvokeCodeBlockWithNullDeclaringClassAndNullTargetClass() {
		MethodSpec method = createTestMethod("methodName", new TypeName[0]);
		MethodReference methodReference = new DefaultMethodReference(method, null);
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none()))
				.isEqualTo(CodeBlock.of("methodName()"));
	}

	@Test
	void toInvokeCodeBlockWithDeclaringClassAndNullTargetClass() {
		MethodSpec method = createTestMethod("methodName", new TypeName[0]);
		MethodReference methodReference = new DefaultMethodReference(method, TEST_CLASS_NAME);
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none()))
				.isEqualTo(CodeBlock.of("new com.example.Test().methodName()"));
	}

	@Test
	void toInvokeCodeBlockWithMatchingTargetClass() {
		MethodSpec method = createTestMethod("methodName", new TypeName[0]);
		MethodReference methodReference = new DefaultMethodReference(method, TEST_CLASS_NAME);
		CodeBlock invocation = methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME);
		// Assume com.example.Test is in a `test` variable.
		assertThat(CodeBlock.of("$L.$L", "test", invocation)).isEqualTo(CodeBlock.of("test.methodName()"));
	}

	@Test
	void toInvokeCodeBlockWithNonMatchingDeclaringClass() {
		MethodSpec method = createTestMethod("methodName", new TypeName[0]);
		MethodReference methodReference = new DefaultMethodReference(method, TEST_CLASS_NAME);
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), INITIALIZER_CLASS_NAME))
				.isEqualTo(CodeBlock.of("new com.example.Test().methodName()"));
	}

	@Test
	void toInvokeCodeBlockWithMatchingArg() {
		MethodReference methodReference = createLocalMethodReference("methodName", ClassName.get(String.class));
		ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator.of(String.class, "stringArg");
		assertThat(methodReference.toInvokeCodeBlock(argCodeGenerator))
				.isEqualTo(CodeBlock.of("methodName(stringArg)"));
	}

	@Test
	void toInvokeCodeBlockWithMatchingArgs() {
		MethodReference methodReference = createLocalMethodReference("methodName",
				ClassName.get(Integer.class), ClassName.get(String.class));
		ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator.of(String.class, "stringArg")
				.and(Integer.class, "integerArg");
		assertThat(methodReference.toInvokeCodeBlock(argCodeGenerator))
				.isEqualTo(CodeBlock.of("methodName(integerArg, stringArg)"));
	}

	@Test
	void toInvokeCodeBlockWithNonMatchingArg() {
		MethodReference methodReference = createLocalMethodReference("methodName",
				ClassName.get(Integer.class), ClassName.get(String.class));
		ArgumentCodeGenerator argCodeGenerator = ArgumentCodeGenerator.of(Integer.class, "integerArg");
		assertThatIllegalArgumentException().isThrownBy(() -> methodReference.toInvokeCodeBlock(argCodeGenerator))
				.withMessageContaining("parameter 1 of type java.lang.String is not supported");
	}

	@Test
	void toInvokeCodeBlockWithStaticMethodAndMatchingDeclaringClass() {
		MethodReference methodReference = createStaticMethodReference("methodName", TEST_CLASS_NAME);
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), TEST_CLASS_NAME))
				.isEqualTo(CodeBlock.of("methodName()"));
	}

	@Test
	void toInvokeCodeBlockWithStaticMethodAndSeparateDeclaringClass() {
		MethodReference methodReference = createStaticMethodReference("methodName", TEST_CLASS_NAME);
		assertThat(methodReference.toInvokeCodeBlock(ArgumentCodeGenerator.none(), INITIALIZER_CLASS_NAME))
				.isEqualTo(CodeBlock.of("com.example.Test.methodName()"));
	}


	private MethodReference createLocalMethodReference(String name, TypeName... argumentTypes) {
		return createMethodReference(name, argumentTypes, null);
	}

	private MethodReference createMethodReference(String name, TypeName[] argumentTypes, @Nullable ClassName declaringClass) {
		MethodSpec method = createTestMethod(name, argumentTypes);
		return new DefaultMethodReference(method, declaringClass);
	}

	private MethodReference createStaticMethodReference(String name, ClassName declaringClass, TypeName... argumentTypes) {
		MethodSpec method = createTestMethod(name, argumentTypes, Modifier.STATIC);
		return new DefaultMethodReference(method, declaringClass);
	}

	private MethodSpec createTestMethod(String name, TypeName[] argumentTypes, Modifier... modifiers) {
		Builder method = MethodSpec.methodBuilder(name);
		for (int i = 0; i < argumentTypes.length; i++) {
			method.addParameter(argumentTypes[i], "args" + i);
		}
		method.addModifiers(modifiers);
		return method.build();
	}

}
