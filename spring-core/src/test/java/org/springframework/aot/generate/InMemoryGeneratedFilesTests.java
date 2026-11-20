/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
				.withMessage("META-INF/test already exists");
	}

	@Test
	void getGeneratedFilesReturnsFiles() {
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
