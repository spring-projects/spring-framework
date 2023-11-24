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

import org.junit.jupiter.api.Test;

import org.springframework.aot.generate.GeneratedFiles.Kind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link InMemoryGeneratedFiles}.
 *
 * @author Phillip Webb
 */
class InMemoryGeneratedFilesTests {

	private final InMemoryGeneratedFiles generatedFiles = new InMemoryGeneratedFiles();

	@Test
	void addFileAddsInMemoryFile() throws Exception {
		this.generatedFiles.addResourceFile("META-INF/test", "test");
		assertThat(this.generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
				"META-INF/test")).isEqualTo("test");
	}

	@Test
	void addFileWhenFileAlreadyAddedThrowsException() {
		this.generatedFiles.addResourceFile("META-INF/test", "test");
		assertThatIllegalStateException().isThrownBy(
				() -> this.generatedFiles.addResourceFile("META-INF/test", "test"))
				.withMessage("Path 'META-INF/test' already in use");
	}

	@Test
	void getGeneratedFilesReturnsFiles() throws Exception {
		this.generatedFiles.addResourceFile("META-INF/test1", "test1");
		this.generatedFiles.addResourceFile("META-INF/test2", "test2");
		assertThat(this.generatedFiles.getGeneratedFiles(Kind.RESOURCE))
				.containsKeys("META-INF/test1", "META-INF/test2");
	}

	@Test
	void getGeneratedFileContentWhenFileExistsReturnsContent() throws Exception {
		this.generatedFiles.addResourceFile("META-INF/test", "test");
		assertThat(this.generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
				"META-INF/test")).isEqualTo("test");
	}

	@Test
	void getGeneratedFileContentWhenFileIsMissingReturnsNull() throws Exception {
		this.generatedFiles.addResourceFile("META-INF/test", "test");
		assertThat(this.generatedFiles.getGeneratedFileContent(Kind.RESOURCE,
				"META-INF/missing")).isNull();
	}

	@Test
	void getGeneratedFileWhenFileExistsReturnsInputStreamSource() {
		this.generatedFiles.addResourceFile("META-INF/test", "test");
		assertThat(this.generatedFiles.getGeneratedFile(Kind.RESOURCE, "META-INF/test"))
				.isNotNull();
	}

	@Test
	void getGeneratedFileWhenFileIsMissingReturnsNull() {
		this.generatedFiles.addResourceFile("META-INF/test", "test");
		assertThat(
				this.generatedFiles.getGeneratedFile(Kind.RESOURCE, "META-INF/missing"))
						.isNull();
	}

}
