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

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.aot.generate.GeneratedFiles.FileHandler;
import org.springframework.aot.generate.GeneratedFiles.Kind;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.function.ThrowingConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link FileSystemGeneratedFiles}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class FileSystemGeneratedFilesTests {

	@TempDir
	Path root;

	@Test
	void addFilesCopiesToFileSystem() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		generatedFiles.addSourceFile("com.example.Test", "{}");
		generatedFiles.addResourceFile("META-INF/test", "test");
		generatedFiles.addClassFile("com/example/TestProxy.class",
				new ByteArrayResource("!".getBytes(StandardCharsets.UTF_8)));
		assertThat(this.root.resolve("sources/com/example/Test.java")).content().isEqualTo("{}");
		assertThat(this.root.resolve("resources/META-INF/test")).content().isEqualTo("test");
		assertThat(this.root.resolve("classes/com/example/TestProxy.class")).content().isEqualTo("!");
	}

	@Test
	void addFilesWithCustomRootsCopiesToFileSystem() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(
				kind -> this.root.resolve("the-" + kind));
		generatedFiles.addSourceFile("com.example.Test", "{}");
		generatedFiles.addResourceFile("META-INF/test", "test");
		generatedFiles.addClassFile("com/example/TestProxy.class",
				new ByteArrayResource("!".getBytes(StandardCharsets.UTF_8)));
		assertThat(this.root.resolve("the-SOURCE/com/example/Test.java")).content().isEqualTo("{}");
		assertThat(this.root.resolve("the-RESOURCE/META-INF/test")).content().isEqualTo("test");
		assertThat(this.root.resolve("the-CLASS/com/example/TestProxy.class")).content().isEqualTo("!");
	}

	@Test
	void createWhenRootIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemGeneratedFiles((Path) null))
				.withMessage("'root' must not be null");
	}

	@Test
	void createWhenRootsIsNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemGeneratedFiles((Function<Kind, Path>) null))
				.withMessage("'roots' must not be null");
	}

	@Test
	void createWhenRootsResultsInNullThrowsException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new FileSystemGeneratedFiles(kind -> (kind != Kind.CLASS) ?
						this.root.resolve(kind.toString()) : null))
				.withMessage("'roots' must return a value for all file kinds");
	}

	@Test
	void addFileWhenPathIsOutsideOfRootThrowsException() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		assertPathMustBeRelative(generatedFiles, "/test");
		assertPathMustBeRelative(generatedFiles, "../test");
		assertPathMustBeRelative(generatedFiles, "test/../../test");
	}

	@Test
	void addFileWhenFileAlreadyAddedThrowsException() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		generatedFiles.addResourceFile("META-INF/mydir", "test");
		assertThatIllegalStateException()
				.isThrownBy(() -> generatedFiles.addResourceFile("META-INF/mydir", "test"))
				.withMessageContainingAll("META-INF", "mydir", "already exists");
	}

	@Test
	void handleFileWhenFileExistsProvidesFileHandler() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		generatedFiles.addResourceFile("META-INF/test", "test");
		generatedFiles.handleFile(Kind.RESOURCE, "META-INF/test", handler -> {
			assertThat(handler.exists()).isTrue();
			assertThat(handler.getContent()).isNotNull();
			assertThat(handler.getContent().getInputStream()).hasContent("test");
		});
		assertThat(this.root.resolve("resources/META-INF/test")).content().isEqualTo("test");
	}

	@Test
	void handleFileWhenFileExistsFailsToCreate() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		generatedFiles.addResourceFile("META-INF/mydir", "test");
		ThrowingConsumer<FileHandler> consumer = handler ->
				handler.create(new ByteArrayResource("should fail".getBytes(StandardCharsets.UTF_8)));
		assertThatIllegalStateException()
				.isThrownBy(() -> generatedFiles.handleFile(Kind.RESOURCE, "META-INF/mydir", consumer))
				.withMessageContainingAll("META-INF", "mydir", "already exists");
	}

	@Test
	void handleFileWhenFileExistsCanOverrideContent() {
		FileSystemGeneratedFiles generatedFiles = new FileSystemGeneratedFiles(this.root);
		generatedFiles.addResourceFile("META-INF/mydir", "test");
		generatedFiles.handleFile(Kind.RESOURCE, "META-INF/mydir", handler ->
				handler.override(new ByteArrayResource("overridden".getBytes(StandardCharsets.UTF_8))));
		assertThat(this.root.resolve("resources/META-INF/mydir")).content().isEqualTo("overridden");
	}

	private void assertPathMustBeRelative(FileSystemGeneratedFiles generatedFiles, String path) {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> generatedFiles.addResourceFile(path, "test"))
				.withMessage("'path' must be relative");
	}

}
