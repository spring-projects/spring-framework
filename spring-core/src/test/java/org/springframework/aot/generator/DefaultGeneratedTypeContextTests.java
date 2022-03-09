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

import javax.lang.model.element.Modifier;

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultGeneratedTypeContext}.
 *
 * @author Stephane Nicoll
 */
class DefaultGeneratedTypeContextTests {

	@Test
	void runtimeHints() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.runtimeHints()).isNotNull();
	}

	@Test
	void getGeneratedTypeMatchesGetMainGeneratedTypeForMainPackage() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.getMainGeneratedType().getClassName()).isEqualTo(ClassName.get("com.acme", "Main"));
		assertThat(context.getGeneratedType("com.acme")).isSameAs(context.getMainGeneratedType());
	}

	@Test
	void getMainGeneratedTypeIsLazilyCreated() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.hasGeneratedType("com.acme")).isFalse();
		context.getMainGeneratedType();
		assertThat(context.hasGeneratedType("com.acme")).isTrue();
	}

	@Test
	void getGeneratedTypeRegisterInstance() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		assertThat(context.hasGeneratedType("com.example")).isFalse();
		GeneratedType generatedType = context.getGeneratedType("com.example");
		assertThat(generatedType).isNotNull();
		assertThat(generatedType.getClassName().simpleName()).isEqualTo("Main");
		assertThat(context.hasGeneratedType("com.example")).isTrue();
	}

	@Test
	void getGeneratedTypeReuseInstance() {
		DefaultGeneratedTypeContext context = createComAcmeContext();
		GeneratedType generatedType = context.getGeneratedType("com.example");
		assertThat(generatedType.getClassName().packageName()).isEqualTo("com.example");
		assertThat(context.getGeneratedType("com.example")).isSameAs(generatedType);
	}

	@Test
	void toJavaFilesWithNoTypeIsEmpty() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		assertThat(writerContext.toJavaFiles()).hasSize(0);
	}

	@Test
	void toJavaFilesWithDefaultTypeIsAddedLazily() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		writerContext.getMainGeneratedType();
		assertThat(writerContext.toJavaFiles()).hasSize(1);
	}

	@Test
	void toJavaFilesWithDefaultTypeAndAdditionaTypes() {
		DefaultGeneratedTypeContext writerContext = createComAcmeContext();
		writerContext.getGeneratedType("com.example");
		writerContext.getGeneratedType("com.another");
		writerContext.getGeneratedType("com.another.another");
		assertThat(writerContext.toJavaFiles()).hasSize(3);
	}

	private DefaultGeneratedTypeContext createComAcmeContext() {
		return new DefaultGeneratedTypeContext("com.acme", packageName ->
				GeneratedType.of(ClassName.get(packageName, "Main"), type -> type.addModifiers(Modifier.PUBLIC)));
	}

}
