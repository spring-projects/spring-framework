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

import java.io.IOException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.javapoet.TypeSpec;
import org.springframework.javapoet.TypeSpec.Builder;

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
			new ClassNameGenerator(Object.class));

	@Test
	void createWhenClassNameGeneratorIsNullThrowsException() {
		assertThatIllegalArgumentException().isThrownBy(() -> new GeneratedClasses(null))
				.withMessage("'classNameGenerator' must not be null");
	}

	@Test
	void forFeatureComponentWhenTargetIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.forFeatureComponent("test", null))
				.withMessage("'component' must not be null");
	}

	@Test
	void forFeatureComponentWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.forFeatureComponent("", TestComponent.class))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void forFeatureWhenFeatureNameIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses.forFeature(""))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void generateWhenTypeSpecCustomizerIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedClasses
						.forFeatureComponent("test", TestComponent.class).generate(null))
				.withMessage("'typeSpecCustomizer' must not be null");
	}

	@Test
	void forFeatureUsesDefaultTarget() {
		GeneratedClass generatedClass = this.generatedClasses
				.forFeature("Test").generate(emptyTypeCustomizer);
		assertThat(generatedClass.getName()).hasToString("java.lang.Object__Test");
	}

	@Test
	void forFeatureComponentUsesComponent() {
		GeneratedClass generatedClass = this.generatedClasses
				.forFeatureComponent("Test", TestComponent.class).generate(emptyTypeCustomizer);
		assertThat(generatedClass.getName().toString()).endsWith("TestComponent__Test");
	}

	@Test
	void generateReturnsDifferentInstances() {
		Consumer<Builder> typeCustomizer = mockTypeCustomizer();
		GeneratedClass generatedClass1 = this.generatedClasses
				.forFeatureComponent("one", TestComponent.class).generate(typeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.forFeatureComponent("one", TestComponent.class).generate(typeCustomizer);
		assertThat(generatedClass1).isNotSameAs(generatedClass2);
		assertThat(generatedClass1.getName().simpleName()).endsWith("__One");
		assertThat(generatedClass2.getName().simpleName()).endsWith("__One1");
	}

	@Test
	void getOrGenerateWhenNewReturnsGeneratedMethod() {
		Consumer<Builder> typeCustomizer = mockTypeCustomizer();
		GeneratedClass generatedClass1 = this.generatedClasses
				.forFeatureComponent("one", TestComponent.class).getOrGenerate("facet", typeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.forFeatureComponent("two", TestComponent.class).getOrGenerate("facet", typeCustomizer);
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrGenerateWhenRepeatReturnsSameGeneratedMethod() {
		Consumer<Builder> typeCustomizer = mockTypeCustomizer();
		GeneratedClass generatedClass1 = this.generatedClasses
				.forFeatureComponent("one", TestComponent.class).getOrGenerate("facet", typeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses
				.forFeatureComponent("one", TestComponent.class).getOrGenerate("facet", typeCustomizer);
		GeneratedClass generatedClass3 = this.generatedClasses
				.forFeatureComponent("one", TestComponent.class).getOrGenerate("facet", typeCustomizer);
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2)
				.isSameAs(generatedClass3);
		verifyNoInteractions(typeCustomizer);
		generatedClass1.generateJavaFile();
		verify(typeCustomizer).accept(any());
	}

	@Test
	@SuppressWarnings("unchecked")
	void writeToInvokeTypeSpecCustomizer() throws IOException {
		Consumer<TypeSpec.Builder> typeSpecCustomizer = mock(Consumer.class);
		this.generatedClasses.forFeatureComponent("one", TestComponent.class)
				.generate(typeSpecCustomizer);
		verifyNoInteractions(typeSpecCustomizer);
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		this.generatedClasses.writeTo(generatedFiles);
		verify(typeSpecCustomizer).accept(any());
		assertThat(generatedFiles.getGeneratedFiles(Kind.SOURCE)).hasSize(1);
	}

	@Test
	void withNameUpdatesNamingConventions() {
		GeneratedClass generatedClass1 = this.generatedClasses
				.forFeatureComponent("one", TestComponent.class).generate(emptyTypeCustomizer);
		GeneratedClass generatedClass2 = this.generatedClasses.withName("Another")
				.forFeatureComponent("one", TestComponent.class).generate(emptyTypeCustomizer);
		assertThat(generatedClass1.getName().toString()).endsWith("TestComponent__One");
		assertThat(generatedClass2.getName().toString()).endsWith("TestComponent__AnotherOne");
	}


	@SuppressWarnings("unchecked")
	private Consumer<TypeSpec.Builder> mockTypeCustomizer() {
		return mock(Consumer.class);
	}


	private static class TestComponent {

	}

}
