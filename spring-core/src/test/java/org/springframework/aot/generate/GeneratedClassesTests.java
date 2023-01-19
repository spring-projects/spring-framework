/*
 * Copyright 2002-2023 the original author or authors.
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

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.javapoet.ClassName;
import org.springframework.javapoet.TypeSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link GeneratedClasses}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedClassesTests {

	private static final Consumer<TypeSpec.Builder> emptyTypeCustomizer = type -> {};

	private final GeneratedClasses generatedClasses = new GeneratedClasses(
			new ClassNameGenerator(ClassName.get("com.example", "Test")));

	@Test
	void createWhenClassNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedClasses(null))
				.withMessage("'classNameGenerator' must not be null");
	}

	@Test
	void addForFeatureComponentWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.addForFeatureComponent("",
						TestComponent.class, emptyTypeCustomizer))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void addForFeatureWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.addForFeature("", emptyTypeCustomizer))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void addForFeatureComponentWhenTypeSpecCustomizerIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses
						.addForFeatureComponent("test", TestComponent.class, null))
				.withMessage("'type' must not be null");
	}

	@Test
	void addForFeatureUsesDefaultTarget() {
		GeneratedClass generatedClass = this.generatedClasses.addForFeature("Test", emptyTypeCustomizer);
		assertThat(generatedClass.getName()).hasToString("com.example.Test__Test");
	}

	@Test
	void addForFeatureComponentUsesTarget() {
		GeneratedClass generatedClass = this.generatedClasses
				.addForFeatureComponent("Test", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass.getName().toString()).endsWith("TestComponent__Test");
	}

	@Test
	void addForFeatureComponentWithSameNameReturnsDifferentInstances() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1).isNotSameAs(generatedClass2);
		assertThat(generatedClass1.getName().simpleName()).endsWith("__One");
		assertThat(generatedClass2.getName().simpleName()).endsWith("__One1");
	}

	@Test
	void getOrAddForFeatureComponentWhenNewReturnsGeneratedClass() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAddForFeatureComponent("two", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrAddForFeatureWhenNewReturnsGeneratedClass() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAddForFeature("one", emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAddForFeature("two", emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrAddForFeatureComponentWhenRepeatReturnsSameGeneratedClass() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass3 = this.generatedClasses
				.getOrAddForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
				.isSameAs(generatedClass3);
	}

	@Test
	void getOrAddForFeatureWhenRepeatReturnsSameGeneratedClass() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.getOrAddForFeature("one", emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.getOrAddForFeature("one", emptyTypeCustomizer);
		GeneratedClass generatedClass3 = this.generatedClasses
				.getOrAddForFeature("one", emptyTypeCustomizer);
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
				.isSameAs(generatedClass3);
	}

	@Test
	void getOrAddForFeatureComponentWhenHasFeatureNamePrefix() {
		GeneratedClasses prefixed = this.generatedClasses.withFeatureNamePrefix("prefix");
		GeneratedClass generatedClass1 = this.generatedClasses.getOrAddForFeatureComponent(
				"one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses.getOrAddForFeatureComponent(
				"one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass3 = prefixed.getOrAddForFeatureComponent
				("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass4 = prefixed.getOrAddForFeatureComponent(
				"one", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1).isSameAs(generatedClass2).isNotSameAs(generatedClass3);
		assertThat(generatedClass3).isSameAs(generatedClass4);
	}

	@Test
	@SuppressWarnings("unchecked")
	void writeToInvokeTypeSpecCustomizer() throws IOException {
		Consumer<TypeSpec.Builder> typeSpecCustomizer = mock();
		this.generatedClasses.addForFeatureComponent("one", TestComponent.class, typeSpecCustomizer);
		verifyNoInteractions(typeSpecCustomizer);
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		this.generatedClasses.writeTo(generatedFiles);
		verify(typeSpecCustomizer).accept(any());
		assertThat(generatedFiles.getGeneratedFiles(Kind.SOURCE)).hasSize(1);
	}

	@Test
	void withNameUpdatesNamingConventions() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses.withFeatureNamePrefix("Another")
				.addForFeatureComponent("one", TestComponent.class, emptyTypeCustomizer);
		assertThat(generatedClass1.getName().toString()).endsWith("TestComponent__One");
		assertThat(generatedClass2.getName().toString()).endsWith("TestComponent__AnotherOne");
	}


	private static class TestComponent {

	}

}
