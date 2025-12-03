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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.core.io.InputStreamSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GeneratedResource}.
 *
 * @author Stephane Nicoll
 */
class GeneratedResourceTests {

	private static final String TEST_RESOURCE_PATH = "com/example/one.properties";

	@Test
	void emptyResourceDoesNotExist() {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		assertThat(resource.getPath()).isEqualTo(TEST_RESOURCE_PATH);
		assertThat(resource.hasContent()).isFalse();
	}

	@Test
	void handleResourceHasContent() {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.create("test=1"));
		assertThat(resource.hasContent()).isTrue();
	}

	@Test
	void createResourceWithCharSequence() throws IOException {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.create("test=1"));
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, TEST_RESOURCE_PATH)).isEqualTo("test=1");
	}

	@Test
	void createResourceWithAppendableCallback() throws IOException {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.create(appendable -> appendable.append("test").append("=").append("1")));
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, TEST_RESOURCE_PATH)).isEqualTo("test=1");
	}

	@Test
	void createResourceWithNullInputStreamSource() throws IOException {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		byte[] content = "test=1".getBytes(StandardCharsets.UTF_8);
		InputStreamSource source = () -> new ByteArrayInputStream(content);
		resource.handle(writer -> writer.create(source));
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, TEST_RESOURCE_PATH)).isEqualTo("test=1");
	}

	@Test
	void createResourceInvokedTwiceWithSameContent() {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.create("test=1"));
		assertThatIllegalStateException()
				.isThrownBy(() -> resource.handle(writer -> writer.create(appendable -> appendable.append("test=1"))))
				.withMessage("Content for generated resource at 'com/example/one.properties' has already been set");
	}

	@Test
	void createOrValidateWithNoContent() throws IOException {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.createOrValidate(appendable -> appendable.append("test").append("=").append("1")));
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, TEST_RESOURCE_PATH)).isEqualTo("test=1");
	}

	@Test
	void createOrValidateWithStringInvokedTwiceWithSameContent() throws IOException {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.createOrValidate(appendable -> appendable.append("test").append("=").append("1")));
		resource.handle(writer -> writer.createOrValidate("test=1"));
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, TEST_RESOURCE_PATH)).isEqualTo("test=1");
	}

	@Test
	void createOrValidateWithAppendableInvokedTwiceWithSameContent() throws IOException {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.createOrValidate("test=1"));
		resource.handle(writer -> writer.createOrValidate(appendable -> appendable.append("test").append("=").append("1")));
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, TEST_RESOURCE_PATH)).isEqualTo("test=1");
	}

	@Test
	void createOrValidateWithInputStreamSourceInvokedTwiceWithSameContent() throws IOException {
		InputStreamSource source = new AppendableConsumerInputStreamSource(appendable -> appendable.append("test").append("=").append("1"));
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.createOrValidate(source));
		resource.handle(writer -> writer.createOrValidate(source));
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFileContent(Kind.RESOURCE, TEST_RESOURCE_PATH)).isEqualTo("test=1");
	}

	@ParameterizedTest
	@ValueSource(strings = { "test=2", "", "test=11" })
	void createOrValidateInvokedTwiceWithDifferentContent(String newContent) {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(writer -> writer.createOrValidate(appendable -> appendable.append("test").append("=").append("1")));
		resource.handle(writer -> assertThatIllegalArgumentException()
				.isThrownBy(() -> writer.createOrValidate(newContent))
				.withMessage("Content for generated resource at 'com/example/one.properties' differs from the content that has already been written"));
	}

	@Test
	void writeToWithNewResources() {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		InMemoryGeneratedFiles generatedFiles = applyToGeneratedFiles(resource);
		assertThat(generatedFiles.getGeneratedFiles(Kind.RESOURCE)).isEmpty();
	}

	@Test
	void getInputStreamWhenContentDoesNotExist() {
		GeneratedResource resource = new GeneratedResource(TEST_RESOURCE_PATH);
		resource.handle(content -> assertThatIllegalStateException()
				.isThrownBy(content::getInputStream)
				.withMessage("No content is set for GeneratedResource[path='com/example/one.properties']"));
	}

	InMemoryGeneratedFiles applyToGeneratedFiles(GeneratedResource resource) {
		InMemoryGeneratedFiles files = new InMemoryGeneratedFiles();
		resource.writeTo(files);
		return files;
	}

}
