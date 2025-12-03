/*
 * Copyright 2002-present the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.javapoet.ClassName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GeneratedResource}.
 *
 * @author Stephane Nicoll
 */
class GeneratedResourcesTests {

	private static final String TEST_RESOURCE_PATH = "com/example/one.properties";

	private static final ClassName TEST_COMPONENT = ClassName.get("org.springframework", "Example");

	private final GeneratedResources generatedResources = new GeneratedResources(
			new NameGenerator(ClassName.get("com.example", "Test")));


	@Test
	void getOrAddWhenPathIsEmptyThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedResources.getOrAdd(""))
				.withMessage("'path' must not be empty");
	}

	@Test
	void getOrAddWhenPathWithLeadingSlashThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedResources.getOrAdd("/resource.txt"))
				.withMessage("Invalid classpath location '/resource.txt'");
	}

	@Test
	void getOrAddForFeatureWithEmptyExtensionThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedResources.getOrAddForFeature("", "test"))
				.withMessage("'extension' must not be empty");
	}

	@Test
	void getOrAddForFeatureWithEmptyFeatureThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> this.generatedResources.getOrAddForFeature("txt", ""))
				.withMessage("'featureName' must not be empty");
	}

	@Test
	void getOrAddForUsesProvidedPath() {
		GeneratedResource generatedClass = this.generatedResources.getOrAdd("META-INF/test.properties");
		assertThat(generatedClass.getPath()).isEqualTo("META-INF/test.properties");
	}

	@Test
	void getOrAddWWhenNewReturnsEmptyGeneratedResource() {
		GeneratedResource generatedResource = this.generatedResources.getOrAdd(TEST_RESOURCE_PATH);
		assertThat(generatedResource.getPath()).isEqualTo(TEST_RESOURCE_PATH);
		assertThat(generatedResource.hasContent()).isFalse();
	}

	@Test
	void getOrAddWhenRepeatReturnsSameGeneratedResource() {
		GeneratedResource generatedResource1 = this.generatedResources.getOrAdd(TEST_RESOURCE_PATH);
		GeneratedResource generatedResource2 = this.generatedResources.getOrAdd(TEST_RESOURCE_PATH);
		GeneratedResource generatedResource3 = this.generatedResources.getOrAdd(TEST_RESOURCE_PATH);
		assertThat(generatedResource1).isNotNull().isSameAs(generatedResource2).isSameAs(generatedResource3);
	}

	@Test
	void getOrAddForFeatureUsesDefaultTarget() {
		GeneratedResource generatedClass = this.generatedResources.getOrAddForFeature("txt", "one");
		assertThat(generatedClass.getPath()).isEqualTo("com/example/Test-one.txt");
	}

	@Test
	void getOrAddForFeatureWhenNewReturnsGeneratedResource() {
		GeneratedResource generatedClass1 = this.generatedResources.getOrAddForFeature("txt", "one");
		GeneratedResource generatedClass2 = this.generatedResources.getOrAddForFeature("zip", "one");
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrAddForFeatureWhenRepeatReturnsSameGeneratedResource() {
		GeneratedResource generatedClass1 = this.generatedResources.getOrAddForFeature("txt", "one");
		GeneratedResource generatedClass2 = this.generatedResources.getOrAddForFeature("txt", "one");
		GeneratedResource generatedClass3 = this.generatedResources.getOrAddForFeature("txt", "one");
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2).isSameAs(generatedClass3);
	}

	@Test
	void getOrAddForFeatureComponentUsesTarget() {
		GeneratedResource generatedClass = this.generatedResources.getOrAddForFeatureComponent("txt", "one",
				TEST_COMPONENT);
		assertThat(generatedClass.getPath()).isEqualTo("org/springframework/Example-one.txt");
	}

	@Test
	void getOrAddForFeatureComponentWhenNewReturnsGeneratedResource() {
		GeneratedResource generatedClass1 = this.generatedResources.getOrAddForFeatureComponent("txt", "one", TEST_COMPONENT);
		GeneratedResource generatedClass2 = this.generatedResources.getOrAddForFeatureComponent("zip", "one", TEST_COMPONENT);
		assertThat(generatedClass1).isNotNull().isNotEqualTo(generatedClass2);
		assertThat(generatedClass2).isNotNull();
	}

	@Test
	void getOrAddForFeatureComponentWhenRepeatReturnsSameGeneratedResource() {
		GeneratedResource generatedClass1 = this.generatedResources.getOrAddForFeatureComponent("txt", "one", TEST_COMPONENT);
		GeneratedResource generatedClass2 = this.generatedResources.getOrAddForFeatureComponent("txt", "one", TEST_COMPONENT);
		GeneratedResource generatedClass3 = this.generatedResources.getOrAddForFeatureComponent("txt", "one", TEST_COMPONENT);
		assertThat(generatedClass1).isNotNull().isSameAs(generatedClass2).isSameAs(generatedClass3);
	}

	@Test
	void addForFeatureWithSameNameReturnsDifferentInstances() {
		GeneratedResource generatedResource1 = this.generatedResources
				.addForFeature("txt", "one");
		GeneratedResource generatedResource2 = this.generatedResources
				.addForFeature("txt", "one");
		assertThat(generatedResource1).isNotSameAs(generatedResource2);
		assertThat(generatedResource1.getPath()).endsWith("-one.txt");
		assertThat(generatedResource2.getPath()).endsWith("-one1.txt");
	}

	@Test
	void addForFeatureComponentWithSameNameReturnsDifferentInstances() {
		GeneratedResource generatedResource1 = this.generatedResources
				.addForFeatureComponent("txt", "one", TEST_COMPONENT);
		GeneratedResource generatedResource2 = this.generatedResources
				.addForFeatureComponent("txt", "one", TEST_COMPONENT);
		assertThat(generatedResource1).isNotSameAs(generatedResource2);
		assertThat(generatedResource1.getPath()).endsWith("-one.txt");
		assertThat(generatedResource2.getPath()).endsWith("-one1.txt");
	}

	@Test
	void withFeatureNameUpdatesNamingConventions() {
		GeneratedResource generatedResources1 = this.generatedResources
				.addForFeatureComponent("txt", "one", TEST_COMPONENT);
		GeneratedResource generatedResources2 = this.generatedResources.withFeatureNamePrefix("another")
				.addForFeatureComponent("txt", "one", TEST_COMPONENT);
		assertThat(generatedResources1.getPath()).endsWith("Example-one.txt");
		assertThat(generatedResources2.getPath()).endsWith("Example-another-one.txt");
	}

	@Test
	void writeToAddResources() {
		this.generatedResources.addForFeatureComponent("txt", "one", TEST_COMPONENT)
				.handle(this::createTestContent);
		this.generatedResources.addForFeatureComponent("json", "two", TEST_COMPONENT)
				.handle(this::createTestContent);
		InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();
		this.generatedResources.writeTo(generatedFiles);
		assertThat(generatedFiles.getGeneratedFiles(GeneratedFiles.Kind.RESOURCE)).containsOnlyKeys(
				"org/springframework/Example-one.txt", "org/springframework/Example-two.json");
	}

	private void createTestContent(GeneratedResource.Content content) {
		content.create("test");
	}


}
