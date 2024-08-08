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

package org.springframework.beans.factory.aot;

import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.beans.testfixture.beans.GenericBean;
import org.springframework.beans.testfixture.beans.factory.aot.DeferredTypeBuilder;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedBean;
import org.springframework.beans.testfixture.beans.factory.generator.deprecation.DeprecatedForRemovalBean;
import org.springframework.core.ResolvableType;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.javapoet.FieldSpec;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CodeWarnings}.
 *
 * @author Stephane Nicoll
 */
class CodeWarningsTests {

	private static final TestCompiler TEST_COMPILER = TestCompiler.forSystem()
			.withCompilerOptions("-Xlint:all", "-Werror");

	private final CodeWarnings codeWarnings = new CodeWarnings();

	private final TestGenerationContext generationContext = new TestGenerationContext();


	@Test
	void registerNoWarningDoesNotIncludeAnnotationOnMethod() {
		compileWithMethod(method -> {
			this.codeWarnings.suppress(method);
			method.addStatement("$T bean = $S", String.class, "Hello");
		}, compiled -> assertThat(compiled.getSourceFile()).doesNotContain("@SuppressWarnings"));
	}

	@Test
	void registerNoWarningDoesNotIncludeAnnotationOnType() {
		compile(type -> {
			this.codeWarnings.suppress(type);
			type.addField(FieldSpec.builder(String.class, "type").build());
		}, compiled -> assertThat(compiled.getSourceFile()).doesNotContain("@SuppressWarnings"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void registerWarningSuppressesItOnMethod() {
		this.codeWarnings.register("deprecation");
		compileWithMethod(method -> {
			this.codeWarnings.suppress(method);
			method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
		}, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings(\"deprecation\")"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void registerWarningSuppressesItOnType() {
		this.codeWarnings.register("deprecation");
		compile(type -> {
			this.codeWarnings.suppress(type);
			type.addField(FieldSpec.builder(DeprecatedBean.class, "bean").build());
		}, compiled -> assertThat(compiled.getSourceFile())
				.contains("@SuppressWarnings(\"deprecation\")"));
	}

	@Test
	@SuppressWarnings({ "deprecation", "removal" })
	void registerSeveralWarningsSuppressesThemOnMethod() {
		this.codeWarnings.register("deprecation");
		this.codeWarnings.register("removal");
		compileWithMethod(method -> {
			this.codeWarnings.suppress(method);
			method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
			method.addStatement("$T another = new $T()", DeprecatedForRemovalBean.class, DeprecatedForRemovalBean.class);
		}, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings({ \"deprecation\", \"removal\" })"));
	}

	@Test
	@SuppressWarnings({ "deprecation", "removal" })
	void registerSeveralWarningsSuppressesThemOnType() {
		this.codeWarnings.register("deprecation");
		this.codeWarnings.register("removal");
		compile(type -> {
			this.codeWarnings.suppress(type);
			type.addField(FieldSpec.builder(DeprecatedBean.class, "bean").build());
			type.addField(FieldSpec.builder(DeprecatedForRemovalBean.class, "another").build());
		}, compiled -> assertThat(compiled.getSourceFile())
				.contains("@SuppressWarnings({ \"deprecation\", \"removal\" })"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void detectDeprecationOnAnnotatedElementWithDeprecated() {
		this.codeWarnings.detectDeprecation(DeprecatedBean.class);
		assertThat(this.codeWarnings.getWarnings()).containsOnly("deprecation");
	}

	@Test
	@SuppressWarnings("deprecation")
	void detectDeprecationOnAnnotatedElementWhoseEnclosingElementIsDeprecated() {
		this.codeWarnings.detectDeprecation(DeprecatedBean.Nested.class);
		assertThat(this.codeWarnings.getWarnings()).containsExactly("deprecation");
	}

	@Test
	@SuppressWarnings("removal")
	void detectDeprecationOnAnnotatedElementWithDeprecatedForRemoval() {
		this.codeWarnings.detectDeprecation(DeprecatedForRemovalBean.class);
		assertThat(this.codeWarnings.getWarnings()).containsOnly("removal");
	}

	@Test
	@SuppressWarnings("removal")
	void detectDeprecationOnAnnotatedElementWhoseEnclosingElementIsDeprecatedForRemoval() {
		this.codeWarnings.detectDeprecation(DeprecatedForRemovalBean.Nested.class);
		assertThat(this.codeWarnings.getWarnings()).containsExactly("removal");
	}

	@ParameterizedTest
	@MethodSource("resolvableTypesWithDeprecated")
	void detectDeprecationOnResolvableTypeWithDeprecated(ResolvableType resolvableType) {
		this.codeWarnings.detectDeprecation(resolvableType);
		assertThat(this.codeWarnings.getWarnings()).containsExactly("deprecation");
	}

	@SuppressWarnings("deprecation")
	static Stream<Arguments> resolvableTypesWithDeprecated() {
		Class<?> deprecatedBean = DeprecatedBean.class;
		Class<?> nested = DeprecatedBean.Nested.class;
		return Stream.of(
				Arguments.of(ResolvableType.forClass(deprecatedBean)),
				Arguments.of(ResolvableType.forClass(nested)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, nested)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
						ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean))),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
						ResolvableType.forClassWithGenerics(GenericBean.class, nested)))
		);
	}

	@ParameterizedTest
	@MethodSource("resolvableTypesWithDeprecatedForRemoval")
	void detectDeprecationOnResolvableTypeWithDeprecatedForRemoval(ResolvableType resolvableType) {
		this.codeWarnings.detectDeprecation(resolvableType);
		assertThat(this.codeWarnings.getWarnings()).containsExactly("removal");
	}

	@SuppressWarnings("removal")
	static Stream<Arguments> resolvableTypesWithDeprecatedForRemoval() {
		Class<?> deprecatedBean = DeprecatedForRemovalBean.class;
		Class<?> nested = DeprecatedForRemovalBean.Nested.class;
		return Stream.of(
				Arguments.of(ResolvableType.forClass(deprecatedBean)),
				Arguments.of(ResolvableType.forClass(nested)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, nested)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
						ResolvableType.forClassWithGenerics(GenericBean.class, deprecatedBean))),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
						ResolvableType.forClassWithGenerics(GenericBean.class, nested)))
		);
	}

	@Test
	void toStringIncludesWarnings() {
		this.codeWarnings.register("deprecation");
		this.codeWarnings.register("rawtypes");
		assertThat(this.codeWarnings).hasToString("CodeWarnings[deprecation, rawtypes]");
	}

	private void compileWithMethod(Consumer<Builder> method, Consumer<Compiled> result) {
		compile(type -> {
			type.addModifiers(Modifier.PUBLIC);
			Builder methodBuilder = MethodSpec.methodBuilder("apply")
					.addModifiers(Modifier.PUBLIC);
			method.accept(methodBuilder);
			type.addMethod(methodBuilder.build());
		}, result);
	}

	private void compile(Consumer<TypeSpec.Builder> type, Consumer<Compiled> result) {
		DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
		this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
		typeBuilder.set(type);
		this.generationContext.writeGeneratedContent();
		TEST_COMPILER.with(this.generationContext).compile(result);
	}

}
