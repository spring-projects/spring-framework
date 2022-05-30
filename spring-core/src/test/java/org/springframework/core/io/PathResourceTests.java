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

package org.springframework.core.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for the {@link PathResource} class.
 *
 * @author Philippe Marschall
 * @author Phillip Webb
 * @author Nicholas Williams
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 */
@Deprecated
class PathResourceTests {

	private static final String TEST_DIR =
			platformPath("src/test/resources/org/springframework/core/io");

	private static final String TEST_FILE =
			platformPath("src/test/resources/org/springframework/core/io/example.properties");

	private static final String NON_EXISTING_FILE =
			platformPath("src/test/resources/org/springframework/core/io/doesnotexist.properties");


	private static String platformPath(String string) {
		return string.replace('/', File.separatorChar);
	}


	@Test
	void nullPath() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PathResource((Path) null))
			.withMessageContaining("Path must not be null");
	}

	@Test
	void nullPathString() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PathResource((String) null))
			.withMessageContaining("Path must not be null");
	}

	@Test
	void nullUri() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PathResource((URI) null))
			.withMessageContaining("URI must not be null");
	}

	@Test
	void createFromPath() {
		Path path = Paths.get(TEST_FILE);
		PathResource resource = new PathResource(path);
		assertThat(resource.getPath()).isEqualTo(TEST_FILE);
	}

	@Test
	void createFromString() {
		PathResource resource = new PathResource(TEST_FILE);
		assertThat(resource.getPath()).isEqualTo(TEST_FILE);
	}

	@Test
	void createFromUri() {
		File file = new File(TEST_FILE);
		PathResource resource = new PathResource(file.toURI());
		assertThat(resource.getPath()).isEqualTo(file.getAbsoluteFile().toString());
	}

	@Test
	void getPathForFile() {
		PathResource resource = new PathResource(TEST_FILE);
		assertThat(resource.getPath()).isEqualTo(TEST_FILE);
	}

	@Test
	void getPathForDir() {
		PathResource resource = new PathResource(TEST_DIR);
		assertThat(resource.getPath()).isEqualTo(TEST_DIR);
	}

	@Test
	void fileExists() {
		PathResource resource = new PathResource(TEST_FILE);
		assertThat(resource.exists()).isTrue();
	}

	@Test
	void dirExists() {
		PathResource resource = new PathResource(TEST_DIR);
		assertThat(resource.exists()).isTrue();
	}

	@Test
	void fileDoesNotExist() {
		PathResource resource = new PathResource(NON_EXISTING_FILE);
		assertThat(resource.exists()).isFalse();
	}

	@Test
	void fileIsReadable() {
		PathResource resource = new PathResource(TEST_FILE);
		assertThat(resource.isReadable()).isTrue();
	}

	@Test
	void doesNotExistIsNotReadable() {
		PathResource resource = new PathResource(NON_EXISTING_FILE);
		assertThat(resource.isReadable()).isFalse();
	}

	@Test
	void directoryIsNotReadable() {
		PathResource resource = new PathResource(TEST_DIR);
		assertThat(resource.isReadable()).isFalse();
	}

	@Test
	void getInputStream() throws IOException {
		PathResource resource = new PathResource(TEST_FILE);
		byte[] bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
		assertThat(bytes.length).isGreaterThan(0);
	}

	@Test
	void getInputStreamForDir() throws IOException {
		PathResource resource = new PathResource(TEST_DIR);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::getInputStream);
	}

	@Test
	void getInputStreamDoesNotExist() throws IOException {
		PathResource resource = new PathResource(NON_EXISTING_FILE);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::getInputStream);
	}

	@Test
	void getUrl() throws IOException {
		PathResource resource = new PathResource(TEST_FILE);
		assertThat(resource.getURL().toString()).endsWith("core/io/example.properties");
	}

	@Test
	void getUri() throws IOException {
		PathResource resource = new PathResource(TEST_FILE);
		assertThat(resource.getURI().toString()).endsWith("core/io/example.properties");
	}

	@Test
	void getFile() throws IOException {
		PathResource resource = new PathResource(TEST_FILE);
		File file = new File(TEST_FILE);
		assertThat(resource.getFile().getAbsoluteFile()).isEqualTo(file.getAbsoluteFile());
	}

	@Test
	void getFileUnsupported() throws IOException {
		Path path = mock(Path.class);
		given(path.normalize()).willReturn(path);
		given(path.toFile()).willThrow(new UnsupportedOperationException());
		PathResource resource = new PathResource(path);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::getFile);
	}

	@Test
	void contentLength() throws IOException {
		PathResource resource = new PathResource(TEST_FILE);
		File file = new File(TEST_FILE);
		assertThat(resource.contentLength()).isEqualTo(file.length());
	}

	@Test
	void contentLengthForDirectory() throws IOException {
		PathResource resource = new PathResource(TEST_DIR);
		File file = new File(TEST_DIR);
		assertThat(resource.contentLength()).isEqualTo(file.length());
	}

	@Test
	void lastModified() throws IOException {
		PathResource resource = new PathResource(TEST_FILE);
		File file = new File(TEST_FILE);
		assertThat(resource.lastModified() / 1000).isEqualTo(file.lastModified() / 1000);
	}

	@Test
	void createRelativeFromDir() throws IOException {
		Resource resource = new PathResource(TEST_DIR).createRelative("example.properties");
		assertThat(resource).isEqualTo(new PathResource(TEST_FILE));
	}

	@Test
	void createRelativeFromFile() throws IOException {
		Resource resource = new PathResource(TEST_FILE).createRelative("../example.properties");
		assertThat(resource).isEqualTo(new PathResource(TEST_FILE));
	}

	@Test
	void filename() {
		Resource resource = new PathResource(TEST_FILE);
		assertThat(resource.getFilename()).isEqualTo("example.properties");
	}

	@Test
	void description() {
		Resource resource = new PathResource(TEST_FILE);
		assertThat(resource.getDescription()).contains("path [");
		assertThat(resource.getDescription()).contains(TEST_FILE);
	}

	@Test
	void fileIsWritable() {
		PathResource resource = new PathResource(TEST_FILE);
		assertThat(resource.isWritable()).isTrue();
	}

	@Test
	void directoryIsNotWritable() {
		PathResource resource = new PathResource(TEST_DIR);
		assertThat(resource.isWritable()).isFalse();
	}

	@Test
	void outputStream(@TempDir Path temporaryFolder) throws IOException {
		PathResource resource = new PathResource(temporaryFolder.resolve("test"));
		FileCopyUtils.copy("test".getBytes(StandardCharsets.UTF_8), resource.getOutputStream());
		assertThat(resource.contentLength()).isEqualTo(4L);
	}

	@Test
	void doesNotExistOutputStream(@TempDir Path temporaryFolder) throws IOException {
		File file = temporaryFolder.resolve("test").toFile();
		file.delete();
		PathResource resource = new PathResource(file.toPath());
		FileCopyUtils.copy("test".getBytes(), resource.getOutputStream());
		assertThat(resource.contentLength()).isEqualTo(4L);
	}

	@Test
	void directoryOutputStream() throws IOException {
		PathResource resource = new PathResource(TEST_DIR);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::getOutputStream);
	}

	@Test
	void getReadableByteChannel() throws IOException {
		PathResource resource = new PathResource(TEST_FILE);
		try (ReadableByteChannel channel = resource.readableChannel()) {
			ByteBuffer buffer = ByteBuffer.allocate((int) resource.contentLength());
			channel.read(buffer);
			buffer.rewind();
			assertThat(buffer.limit()).isGreaterThan(0);
		}
	}

	@Test
	void getReadableByteChannelForDir() throws IOException {
		PathResource resource = new PathResource(TEST_DIR);
		try {
			resource.readableChannel();
		}
		catch (AccessDeniedException ex) {
			// on Windows
		}
	}

	@Test
	void getReadableByteChannelDoesNotExist() throws IOException {
		PathResource resource = new PathResource(NON_EXISTING_FILE);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::readableChannel);
	}

	@Test
	void getWritableChannel(@TempDir Path temporaryFolder) throws IOException {
		Path testPath = temporaryFolder.resolve("test");
		Files.createFile(testPath);
		PathResource resource = new PathResource(testPath);
		ByteBuffer buffer = ByteBuffer.wrap("test".getBytes(StandardCharsets.UTF_8));
		try (WritableByteChannel channel = resource.writableChannel()) {
			channel.write(buffer);
		}
		assertThat(resource.contentLength()).isEqualTo(4L);
	}

}
