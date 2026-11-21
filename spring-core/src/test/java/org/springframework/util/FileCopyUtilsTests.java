/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileCopyUtils}.
 *
 * @author Juergen Hoeller
 * @since 12.03.2005
 */
class FileCopyUtilsTests {

	@Test
	void copyFromInputStream() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
		int count = FileCopyUtils.copy(in, out);
		assertThat(count).isEqualTo(content.length);
		assertThat(out.toByteArray()).isEqualTo(content);
	}

	@Test
	void copyFromByteArray() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayOutputStream out = new ByteArrayOutputStream(content.length);
		FileCopyUtils.copy(content, out);
		assertThat(out.toByteArray()).isEqualTo(content);
	}

	@Test
	void copyToByteArray() throws IOException {
		byte[] content = "content".getBytes();
		ByteArrayInputStream in = new ByteArrayInputStream(content);
		byte[] result = FileCopyUtils.copyToByteArray(in);
		assertThat(result).isEqualTo(content);
	}

	@Test
	void copyFromReader() throws IOException {
		String content = "content";
		StringReader in = new StringReader(content);
		StringWriter out = new StringWriter();
		int count = FileCopyUtils.copy(in, out);
		assertThat(count).isEqualTo(content.length());
		assertThat(out.toString()).isEqualTo(content);
	}

	@Test
	void copyFromString() throws IOException {
		String content = "content";
		StringWriter out = new StringWriter();
		FileCopyUtils.copy(content, out);
		assertThat(out.toString()).isEqualTo(content);
	}

	@Test
	void copyToString() throws IOException {
		String content = "content";
		StringReader in = new StringReader(content);
		String result = FileCopyUtils.copyToString(in);
		assertThat(result).isEqualTo(content);
	}

	@Test
	void copyFile(@TempDir Path tempDir) throws IOException {
		Path source = tempDir.resolve("src");
		Path target = tempDir.resolve("target");
		Files.write(source, "content".getBytes());
		int bytesWritten = FileCopyUtils.copy(source.toFile(), target.toFile());
		assertThat(bytesWritten).isEqualTo(7);
		assertThat(target).exists();
		assertThat(target).content().isEqualTo("content");
	}

	@Test
	void copyFileToByteArray(@TempDir Path tempDir) throws IOException {
		Path source = tempDir.resolve("src");
		Files.write(source, "content".getBytes());
		assertThat(FileCopyUtils.copyToByteArray(source.toFile())).asString().isEqualTo("content");
	}

	@Test
	void copyFileToSameFile(@TempDir Path tempDir) throws IOException {
		Path source = tempDir.resolve("src");
		Files.write(source, "content".getBytes());
		assertThat(FileCopyUtils.copy(source.toFile(), source.toFile())).isZero();
		assertThat(source).content().isEqualTo("content");
		Path sameFile = tempDir.resolve("sub").resolve("..").resolve("src");
		Files.createDirectory(tempDir.resolve("sub"));
		assertThat(FileCopyUtils.copy(source.toFile(), sameFile.toFile())).isZero();
		assertThat(source).content().isEqualTo("content");
	}

	@Test
	void copyByteArrayToFile(@TempDir Path tempDir) throws IOException {
		Path target = tempDir.resolve("target");
		FileCopyUtils.copy("content".getBytes(), target.toFile());
		assertThat(target).exists();
		assertThat(target).content().isEqualTo("content");
	}
}
