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
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.MethodSpec.Builder;

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
	void registerNoWarningDoesNotIncludeAnnotation() {
		compile(method -> {
			this.codeWarnings.suppress(method);
			method.addStatement("$T bean = $S", String.class, "Hello");
		}, compiled -> assertThat(compiled.getSourceFile()).doesNotContain("@SuppressWarnings"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void registerWarningSuppressesIt() {
		this.codeWarnings.register("deprecation");
		compile(method -> {
			this.codeWarnings.suppress(method);
			method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
		}, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings(\"deprecation\")"));
	}

	@Test
	@SuppressWarnings({ "deprecation", "removal" })
	void registerSeveralWarningsSuppressesThem() {
		this.codeWarnings.register("deprecation");
		this.codeWarnings.register("removal");
		compile(method -> {
			this.codeWarnings.suppress(method);
			method.addStatement("$T bean = new $T()", DeprecatedBean.class, DeprecatedBean.class);
			method.addStatement("$T another = new $T()", DeprecatedForRemovalBean.class, DeprecatedForRemovalBean.class);
		}, compiled -> assertThat(compiled.getSourceFile()).contains("@SuppressWarnings({ \"deprecation\", \"removal\" })"));
	}

	@Test
	@SuppressWarnings("deprecation")
	void detectDeprecationOnAnnotatedElementWithDeprecated() {
		this.codeWarnings.detectDeprecation(DeprecatedBean.class);
		assertThat(this.codeWarnings.getWarnings()).containsOnly("deprecation");
	}

	@Test
	@SuppressWarnings("removal")
	void detectDeprecationOnAnnotatedElementWithDeprecatedForRemoval() {
		this.codeWarnings.detectDeprecation(DeprecatedForRemovalBean.class);
		assertThat(this.codeWarnings.getWarnings()).containsOnly("removal");
	}

	@ParameterizedTest
	@MethodSource("resolvableTypesWithDeprecated")
	void detectDeprecationOnResolvableTypeWithDeprecated(ResolvableType resolvableType) {
		this.codeWarnings.detectDeprecation(resolvableType);
		assertThat(this.codeWarnings.getWarnings()).containsExactly("deprecation");
	}

	@SuppressWarnings("deprecation")
	static Stream<Arguments> resolvableTypesWithDeprecated() {
		return Stream.of(
				Arguments.of(ResolvableType.forClass(DeprecatedBean.class)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedBean.class)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
						ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedBean.class)))
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
		return Stream.of(
				Arguments.of(ResolvableType.forClass(DeprecatedForRemovalBean.class)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedForRemovalBean.class)),
				Arguments.of(ResolvableType.forClassWithGenerics(GenericBean.class,
						ResolvableType.forClassWithGenerics(GenericBean.class, DeprecatedForRemovalBean.class)))
		);
	}

	@Test
	void toStringIncludesWarnings() {
		this.codeWarnings.register("deprecation");
		this.codeWarnings.register("rawtypes");
		assertThat(this.codeWarnings).hasToString("CodeWarnings[deprecation, rawtypes]");
	}

	private void compile(Consumer<Builder> method, Consumer<Compiled> result) {
		DeferredTypeBuilder typeBuilder = new DeferredTypeBuilder();
		this.generationContext.getGeneratedClasses().addForFeature("TestCode", typeBuilder);
		typeBuilder.set(type -> {
			type.addModifiers(Modifier.PUBLIC);
			Builder methodBuilder = MethodSpec.methodBuilder("apply")
					.addModifiers(Modifier.PUBLIC);
			method.accept(methodBuilder);
			type.addMethod(methodBuilder.build());
		});
		this.generationContext.writeGeneratedContent();
		TEST_COMPILER.with(this.generationContext).compile(result);
	}

}
