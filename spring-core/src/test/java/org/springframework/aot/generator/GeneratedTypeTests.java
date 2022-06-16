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

package org.springframework.aot.generator;

import java.io.IOException;
import java.io.StringWriter;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.CodeBlock;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.TypeName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GeneratedType}.
 *
 * @author Stephane Nicoll
 */
class GeneratedTypeTests {

	private static final ClassName TEST_CLASS_NAME = ClassName.get("com.acme", "Test");

	@Test
	void className() {
		GeneratedType generatedType = new GeneratedType(TEST_CLASS_NAME,
				type -> type.addModifiers(Modifier.STATIC));
		assertThat(generatedType.getClassName()).isEqualTo(TEST_CLASS_NAME);
		assertThat(generateCode(generatedType)).contains("static class Test {");
	}

	@Test
	void createWithCustomField() {
		GeneratedType generatedType = new GeneratedType(TEST_CLASS_NAME,
				type -> type.addField(FieldSpec.builder(TypeName.BOOLEAN, "enabled").build()));
		assertThat(generateCode(generatedType)).contains("boolean enabled;");
	}

	@Test
	void customizeType() {
		GeneratedType generatedType = createTestGeneratedType();
		generatedType.customizeType(type -> type.addJavadoc("Test javadoc."))
				.customizeType(type -> type.addJavadoc(" Another test javadoc"));
		assertThat(generateCode(generatedType)).containsSequence(
				"/**\n",
				" * Test javadoc. Another test javadoc\n",
				" */");
	}

	@Test
	void addMethod() {
		GeneratedType generatedType = createTestGeneratedType();
		generatedType.addMethod(MethodSpec.methodBuilder("test").returns(Integer.class)
				.addCode(CodeBlock.of("return 42;")));
		assertThat(generateCode(generatedType)).containsSequence(
				"\tInteger test() {\n",
				"\t\treturn 42;\n",
				"\t}");
	}

	@Test
	void addMultipleMethods() {
		GeneratedType generatedType = createTestGeneratedType();
		generatedType.addMethod(MethodSpec.methodBuilder("first"));
		generatedType.addMethod(MethodSpec.methodBuilder("second"));
		assertThat(generateCode(generatedType))
				.containsSequence("\tvoid first() {\n", "\t}")
				.containsSequence("\tvoid second() {\n", "\t}");
	}

	@Test
	void addSimilarMethodGenerateUniqueNames() {
		GeneratedType generatedType = createTestGeneratedType();
		MethodSpec firstMethod = generatedType.addMethod(MethodSpec.methodBuilder("test"));
		MethodSpec secondMethod = generatedType.addMethod(MethodSpec.methodBuilder("test"));
		MethodSpec thirdMethod = generatedType.addMethod(MethodSpec.methodBuilder("test"));
		assertThat(firstMethod.name).isEqualTo("test");
		assertThat(secondMethod.name).isEqualTo("test_");
		assertThat(thirdMethod.name).isEqualTo("test__");
		assertThat(generateCode(generatedType))
				.containsSequence("\tvoid test() {\n", "\t}")
				.containsSequence("\tvoid test_() {\n", "\t}")
				.containsSequence("\tvoid test__() {\n", "\t}");
	}

	@Test
	void addMethodWithSameNameAndDifferentArgumentsDoesNotChangeName() {
		GeneratedType generatedType = createTestGeneratedType();
		generatedType.addMethod(MethodSpec.methodBuilder("test"));
		MethodSpec secondMethod = generatedType.addMethod(MethodSpec.methodBuilder("test")
				.addParameter(String.class, "param"));
		assertThat(secondMethod.name).isEqualTo("test");
	}

	private GeneratedType createTestGeneratedType() {
		return GeneratedType.of(TEST_CLASS_NAME);
	}

	private String generateCode(GeneratedType generatedType) {
		try {
			StringWriter out = new StringWriter();
			generatedType.toJavaFile().writeTo(out);
			return out.toString();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
