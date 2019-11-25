/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit tests for various {@link Resource} implementations.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 09.09.2004
 */
class ResourceTests {

	@Test
	void byteArrayResource() throws IOException {
		Resource resource = new ByteArrayResource("testString".getBytes());
		assertThat(resource.exists()).isTrue();
		assertThat(resource.isOpen()).isFalse();
		String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		assertThat(content).isEqualTo("testString");
		assertThat(new ByteArrayResource("testString".getBytes())).isEqualTo(resource);
	}

	@Test
	void byteArrayResourceWithDescription() throws IOException {
		Resource resource = new ByteArrayResource("testString".getBytes(), "my description");
		assertThat(resource.exists()).isTrue();
		assertThat(resource.isOpen()).isFalse();
		String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		assertThat(content).isEqualTo("testString");
		assertThat(resource.getDescription().contains("my description")).isTrue();
		assertThat(new ByteArrayResource("testString".getBytes())).isEqualTo(resource);
	}

	@Test
	void inputStreamResource() throws IOException {
		InputStream is = new ByteArrayInputStream("testString".getBytes());
		Resource resource = new InputStreamResource(is);
		assertThat(resource.exists()).isTrue();
		assertThat(resource.isOpen()).isTrue();
		String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		assertThat(content).isEqualTo("testString");
		assertThat(new InputStreamResource(is)).isEqualTo(resource);
	}

	@Test
	void inputStreamResourceWithDescription() throws IOException {
		InputStream is = new ByteArrayInputStream("testString".getBytes());
		Resource resource = new InputStreamResource(is, "my description");
		assertThat(resource.exists()).isTrue();
		assertThat(resource.isOpen()).isTrue();
		String content = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
		assertThat(content).isEqualTo("testString");
		assertThat(resource.getDescription().contains("my description")).isTrue();
		assertThat(new InputStreamResource(is)).isEqualTo(resource);
	}

	@Test
	void classPathResource() throws IOException {
		Resource resource = new ClassPathResource("org/springframework/core/io/Resource.class");
		doTestResource(resource);
		Resource resource2 = new ClassPathResource("org/springframework/core/../core/io/./Resource.class");
		assertThat(resource2).isEqualTo(resource);
		Resource resource3 = new ClassPathResource("org/springframework/core/").createRelative("../core/io/./Resource.class");
		assertThat(resource3).isEqualTo(resource);

		// Check whether equal/hashCode works in a HashSet.
		HashSet<Resource> resources = new HashSet<>();
		resources.add(resource);
		resources.add(resource2);
		assertThat(resources.size()).isEqualTo(1);
	}

	@Test
	void classPathResourceWithClassLoader() throws IOException {
		Resource resource =
				new ClassPathResource("org/springframework/core/io/Resource.class", getClass().getClassLoader());
		doTestResource(resource);
		assertThat(new ClassPathResource("org/springframework/core/../core/io/./Resource.class", getClass().getClassLoader())).isEqualTo(resource);
	}

	@Test
	void classPathResourceWithClass() throws IOException {
		Resource resource = new ClassPathResource("Resource.class", getClass());
		doTestResource(resource);
		assertThat(new ClassPathResource("Resource.class", getClass())).isEqualTo(resource);
	}

	@Test
	void fileSystemResource() throws IOException {
		String file = getClass().getResource("Resource.class").getFile();
		Resource resource = new FileSystemResource(file);
		doTestResource(resource);
		assertThat(resource).isEqualTo(new FileSystemResource(file));
	}

	@Test
	void fileSystemResourceWithFilePath() throws Exception {
		Path filePath = Paths.get(getClass().getResource("Resource.class").toURI());
		Resource resource = new FileSystemResource(filePath);
		doTestResource(resource);
		assertThat(resource).isEqualTo(new FileSystemResource(filePath));
	}

	@Test
	void fileSystemResourceWithPlainPath() {
		Resource resource = new FileSystemResource("core/io/Resource.class");
		assertThat(new FileSystemResource("core/../core/io/./Resource.class")).isEqualTo(resource);
	}

	@Test
	void urlResource() throws IOException {
		Resource resource = new UrlResource(getClass().getResource("Resource.class"));
		doTestResource(resource);
		assertThat(resource).isEqualTo(new UrlResource(getClass().getResource("Resource.class")));

		Resource resource2 = new UrlResource("file:core/io/Resource.class");
		assertThat(new UrlResource("file:core/../core/io/./Resource.class")).isEqualTo(resource2);

		assertThat(new UrlResource("file:/dir/test.txt?argh").getFilename()).isEqualTo("test.txt");
		assertThat(new UrlResource("file:\\dir\\test.txt?argh").getFilename()).isEqualTo("test.txt");
		assertThat(new UrlResource("file:\\dir/test.txt?argh").getFilename()).isEqualTo("test.txt");
	}

	private void doTestResource(Resource resource) throws IOException {
		assertThat(resource.getFilename()).isEqualTo("Resource.class");
		assertThat(resource.getURL().getFile().endsWith("Resource.class")).isTrue();
		assertThat(resource.exists()).isTrue();
		assertThat(resource.isReadable()).isTrue();
		assertThat(resource.contentLength() > 0).isTrue();
		assertThat(resource.lastModified() > 0).isTrue();

		Resource relative1 = resource.createRelative("ClassPathResource.class");
		assertThat(relative1.getFilename()).isEqualTo("ClassPathResource.class");
		assertThat(relative1.getURL().getFile().endsWith("ClassPathResource.class")).isTrue();
		assertThat(relative1.exists()).isTrue();
		assertThat(relative1.isReadable()).isTrue();
		assertThat(relative1.contentLength() > 0).isTrue();
		assertThat(relative1.lastModified() > 0).isTrue();

		Resource relative2 = resource.createRelative("support/ResourcePatternResolver.class");
		assertThat(relative2.getFilename()).isEqualTo("ResourcePatternResolver.class");
		assertThat(relative2.getURL().getFile().endsWith("ResourcePatternResolver.class")).isTrue();
		assertThat(relative2.exists()).isTrue();
		assertThat(relative2.isReadable()).isTrue();
		assertThat(relative2.contentLength() > 0).isTrue();
		assertThat(relative2.lastModified() > 0).isTrue();

		Resource relative3 = resource.createRelative("../SpringVersion.class");
		assertThat(relative3.getFilename()).isEqualTo("SpringVersion.class");
		assertThat(relative3.getURL().getFile().endsWith("SpringVersion.class")).isTrue();
		assertThat(relative3.exists()).isTrue();
		assertThat(relative3.isReadable()).isTrue();
		assertThat(relative3.contentLength() > 0).isTrue();
		assertThat(relative3.lastModified() > 0).isTrue();

		Resource relative4 = resource.createRelative("X.class");
		assertThat(relative4.exists()).isFalse();
		assertThat(relative4.isReadable()).isFalse();
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				relative4::contentLength);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				relative4::lastModified);
	}

	@Test
	void classPathResourceWithRelativePath() throws IOException {
		Resource resource = new ClassPathResource("dir/");
		Resource relative = resource.createRelative("subdir");
		assertThat(relative).isEqualTo(new ClassPathResource("dir/subdir"));
	}

	@Test
	void fileSystemResourceWithRelativePath() throws IOException {
		Resource resource = new FileSystemResource("dir/");
		Resource relative = resource.createRelative("subdir");
		assertThat(relative).isEqualTo(new FileSystemResource("dir/subdir"));
	}

	@Test
	void urlResourceWithRelativePath() throws IOException {
		Resource resource = new UrlResource("file:dir/");
		Resource relative = resource.createRelative("subdir");
		assertThat(relative).isEqualTo(new UrlResource("file:dir/subdir"));
	}

	@Test
	void nonFileResourceExists() throws Exception {
		URL url = new URL("https://spring.io/");

		// Abort if spring.io is not reachable.
		assumeTrue(urlIsReachable(url));

		Resource resource = new UrlResource(url);
		assertThat(resource.exists()).isTrue();
	}

	private boolean urlIsReachable(URL url) {
		try {
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("HEAD");
			connection.setReadTimeout(5_000);
			return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
		}
		catch (Exception ex) {
			return false;
		}
	}

	@Test
	void abstractResourceExceptions() throws Exception {
		final String name = "test-resource";

		Resource resource = new AbstractResource() {
			@Override
			public String getDescription() {
				return name;
			}
			@Override
			public InputStream getInputStream() throws IOException {
				throw new FileNotFoundException();
			}
		};

		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::getURL)
			.withMessageContaining(name);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
				resource::getFile)
			.withMessageContaining(name);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
				resource.createRelative("/testing"))
			.withMessageContaining(name);

		assertThat(resource.getFilename()).isNull();
	}

	@Test
	void contentLength() throws IOException {
		AbstractResource resource = new AbstractResource() {
			@Override
			public InputStream getInputStream() {
				return new ByteArrayInputStream(new byte[] { 'a', 'b', 'c' });
			}
			@Override
			public String getDescription() {
				return "";
			}
		};
		assertThat(resource.contentLength()).isEqualTo(3L);
	}

	@Test
	void readableChannel() throws IOException {
		Resource resource = new FileSystemResource(getClass().getResource("Resource.class").getFile());
		ReadableByteChannel channel = null;
		try {
			channel = resource.readableChannel();
			ByteBuffer buffer = ByteBuffer.allocate((int) resource.contentLength());
			channel.read(buffer);
			buffer.rewind();
			assertThat(buffer.limit() > 0).isTrue();
		}
		finally {
			if (channel != null) {
				channel.close();
			}
		}
	}

	@Test
	void inputStreamNotFoundOnFileSystemResource() throws IOException {
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
				new FileSystemResource(getClass().getResource("Resource.class").getFile()).createRelative("X").getInputStream());
	}

	@Test
	void readableChannelNotFoundOnFileSystemResource() throws IOException {
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
				new FileSystemResource(getClass().getResource("Resource.class").getFile()).createRelative("X").readableChannel());
	}

	@Test
	void inputStreamNotFoundOnClassPathResource() throws IOException {
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
				new ClassPathResource("Resource.class", getClass()).createRelative("X").getInputStream());
	}

	@Test
	void readableChannelNotFoundOnClassPathResource() throws IOException {
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
				new ClassPathResource("Resource.class", getClass()).createRelative("X").readableChannel());
	}

}
