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

package org.springframework.core.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.util.FileCopyUtils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Unit tests for various {@link Resource} implementations.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @author Brian Clozel
 */
class ResourceTests {

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("resource")
	void resourceIsValid(Resource resource) throws Exception {
		assertThat(resource.getFilename()).isEqualTo("ResourceTests.class");
		assertThat(resource.getURL().getFile()).endsWith("ResourceTests.class");
		assertThat(resource.exists()).isTrue();
		assertThat(resource.isReadable()).isTrue();
		assertThat(resource.contentLength()).isGreaterThan(0);
		assertThat(resource.lastModified()).isGreaterThan(0);
		assertThat(resource.getContentAsByteArray()).containsExactly(Files.readAllBytes(Path.of(resource.getURI())));
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("resource")
	void resourceCreateRelative(Resource resource) throws Exception {
		Resource relative1 = resource.createRelative("ClassPathResourceTests.class");
		assertThat(relative1.getFilename()).isEqualTo("ClassPathResourceTests.class");
		assertThat(relative1.getURL().getFile().endsWith("ClassPathResourceTests.class")).isTrue();
		assertThat(relative1.exists()).isTrue();
		assertThat(relative1.isReadable()).isTrue();
		assertThat(relative1.contentLength()).isGreaterThan(0);
		assertThat(relative1.lastModified()).isGreaterThan(0);
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("resource")
	void resourceCreateRelativeWithFolder(Resource resource) throws Exception {
		Resource relative2 = resource.createRelative("support/PathMatchingResourcePatternResolverTests.class");
		assertThat(relative2.getFilename()).isEqualTo("PathMatchingResourcePatternResolverTests.class");
		assertThat(relative2.getURL().getFile()).endsWith("PathMatchingResourcePatternResolverTests.class");
		assertThat(relative2.exists()).isTrue();
		assertThat(relative2.isReadable()).isTrue();
		assertThat(relative2.contentLength()).isGreaterThan(0);
		assertThat(relative2.lastModified()).isGreaterThan(0);
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("resource")
	void resourceCreateRelativeWithDotPath(Resource resource) throws Exception {
		Resource relative3 = resource.createRelative("../CollectionFactoryTests.class");
		assertThat(relative3.getFilename()).isEqualTo("CollectionFactoryTests.class");
		assertThat(relative3.getURL().getFile()).endsWith("CollectionFactoryTests.class");
		assertThat(relative3.exists()).isTrue();
		assertThat(relative3.isReadable()).isTrue();
		assertThat(relative3.contentLength()).isGreaterThan(0);
		assertThat(relative3.lastModified()).isGreaterThan(0);
	}

	@ParameterizedTest(name = "{index}: {0}")
	@MethodSource("resource")
	void resourceCreateRelativeUnknown(Resource resource) throws Exception {
		Resource relative4 = resource.createRelative("X.class");
		assertThat(relative4.exists()).isFalse();
		assertThat(relative4.isReadable()).isFalse();
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::contentLength);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::lastModified);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::getInputStream);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::readableChannel);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(relative4::getContentAsByteArray);
		assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() -> relative4.getContentAsString(UTF_8));
	}

	private static Stream<Arguments> resource() throws URISyntaxException {
		URL resourceClass = ResourceTests.class.getResource("ResourceTests.class");
		Path resourceClassFilePath = Paths.get(resourceClass.toURI());
		return Stream.of(
				arguments(named("ClassPathResource", new ClassPathResource("org/springframework/core/io/ResourceTests.class"))),
				arguments(named("ClassPathResource with ClassLoader", new ClassPathResource("org/springframework/core/io/ResourceTests.class", ResourceTests.class.getClassLoader()))),
				arguments(named("ClassPathResource with Class", new ClassPathResource("ResourceTests.class", ResourceTests.class))),
				arguments(named("FileSystemResource", new FileSystemResource(resourceClass.getFile()))),
				arguments(named("FileSystemResource with File", new FileSystemResource(new File(resourceClass.getFile())))),
				arguments(named("FileSystemResource with File path", new FileSystemResource(resourceClassFilePath))),
				arguments(named("UrlResource", new UrlResource(resourceClass)))
		);
	}


	@Nested
	class ByteArrayResourceTests {

		@Test
		void hasContent() throws Exception {
			String testString = "testString";
			byte[] testBytes = testString.getBytes();
			Resource resource = new ByteArrayResource(testBytes);
			assertThat(resource.exists()).isTrue();
			assertThat(resource.isOpen()).isFalse();
			byte[] contentBytes = resource.getContentAsByteArray();
			assertThat(contentBytes).containsExactly(testBytes);
			String contentString = resource.getContentAsString(StandardCharsets.US_ASCII);
			assertThat(contentString).isEqualTo(testString);
			contentString = FileCopyUtils.copyToString(new InputStreamReader(resource.getInputStream()));
			assertThat(contentString).isEqualTo(testString);
			assertThat(new ByteArrayResource(testBytes)).isEqualTo(resource);
		}

		@Test
		void isNotOpen() {
			Resource resource = new ByteArrayResource("testString".getBytes());
			assertThat(resource.exists()).isTrue();
			assertThat(resource.isOpen()).isFalse();
		}

		@Test
		void hasDescription() {
			Resource resource = new ByteArrayResource("testString".getBytes(), "my description");
			assertThat(resource.getDescription().contains("my description")).isTrue();
		}

	}

	@Nested
	class InputStreamResourceTests {

		@Test
		void hasContent() throws Exception {
			String testString = "testString";
			byte[] testBytes = testString.getBytes();
			InputStream is = new ByteArrayInputStream(testBytes);
			Resource resource1 = new InputStreamResource(is);
			String content = FileCopyUtils.copyToString(new InputStreamReader(resource1.getInputStream()));
			assertThat(content).isEqualTo(testString);
			assertThat(new InputStreamResource(is)).isEqualTo(resource1);
			assertThatIllegalStateException().isThrownBy(resource1::getInputStream);

			Resource resource2 = new InputStreamResource(new ByteArrayInputStream(testBytes));
			assertThat(resource2.getContentAsByteArray()).containsExactly(testBytes);
			assertThatIllegalStateException().isThrownBy(resource2::getContentAsByteArray);

			Resource resource3 = new InputStreamResource(new ByteArrayInputStream(testBytes));
			assertThat(resource3.getContentAsString(StandardCharsets.US_ASCII)).isEqualTo(testString);
			assertThatIllegalStateException().isThrownBy(() -> resource3.getContentAsString(StandardCharsets.US_ASCII));
		}

		@Test
		void isOpen() {
			InputStream is = new ByteArrayInputStream("testString".getBytes());
			Resource resource = new InputStreamResource(is);
			assertThat(resource.exists()).isTrue();
			assertThat(resource.isOpen()).isTrue();
		}

		@Test
		void hasDescription() {
			InputStream is = new ByteArrayInputStream("testString".getBytes());
			Resource resource = new InputStreamResource(is, "my description");
			assertThat(resource.getDescription().contains("my description")).isTrue();
		}
	}

	@Nested
	class FileSystemResourceTests {

		@Test
		void sameResourceIsEqual() {
			String file = getClass().getResource("ResourceTests.class").getFile();
			Resource resource = new FileSystemResource(file);
			assertThat(resource).isEqualTo(new FileSystemResource(file));
		}

		@Test
		void sameResourceFromFileIsEqual() {
			File file = new File(getClass().getResource("ResourceTests.class").getFile());
			Resource resource = new FileSystemResource(file);
			assertThat(resource).isEqualTo(new FileSystemResource(file));
		}

		@Test
		void sameResourceFromFilePathIsEqual() throws Exception {
			Path filePath = Paths.get(getClass().getResource("ResourceTests.class").toURI());
			Resource resource = new FileSystemResource(filePath);
			assertThat(resource).isEqualTo(new FileSystemResource(filePath));
		}

		@Test
		void sameResourceFromDotPathIsEqual() {
			Resource resource = new FileSystemResource("core/io/ResourceTests.class");
			assertThat(new FileSystemResource("core/../core/io/./ResourceTests.class")).isEqualTo(resource);
		}

		@Test
		void relativeResourcesAreEqual() throws Exception {
			Resource resource = new FileSystemResource("dir/");
			Resource relative = resource.createRelative("subdir");
			assertThat(relative).isEqualTo(new FileSystemResource("dir/subdir"));
		}

		@Test
		void readableChannelProvidesContent() throws Exception {
			Resource resource = new FileSystemResource(getClass().getResource("ResourceTests.class").getFile());
			try (ReadableByteChannel channel = resource.readableChannel()) {
				ByteBuffer buffer = ByteBuffer.allocate((int) resource.contentLength());
				channel.read(buffer);
				buffer.rewind();
				assertThat(buffer.limit()).isGreaterThan(0);
			}
		}

		@Test
		void urlAndUriAreNormalizedWhenCreatedFromFile() throws Exception {
			Path path = Path.of("src/test/resources/scanned-resources/resource#test1.txt").toAbsolutePath();
			assertUrlAndUriBehavior(new FileSystemResource(path.toFile()));
		}

		@Test
		void urlAndUriAreNormalizedWhenCreatedFromPath() throws Exception {
			Path path = Path.of("src/test/resources/scanned-resources/resource#test1.txt").toAbsolutePath();
			assertUrlAndUriBehavior(new FileSystemResource(path));
		}

		/**
		 * The following assertions serve as regression tests for the lack of the
		 * "authority component" (//) in the returned URI/URL. For example, we are
		 * expecting file:/my/path (or file:/C:/My/Path) instead of file:///my/path.
		 */
		private void assertUrlAndUriBehavior(Resource resource) throws IOException {
			assertThat(resource.getURL().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
			assertThat(resource.getURI().toString()).matches("^file:\\/[^\\/].+test1\\.txt$");
		}
	}

	@Nested
	class UrlResourceTests {

		private MockWebServer server = new MockWebServer();

		@Test
		void sameResourceWithRelativePathIsEqual() throws Exception {
			Resource resource = new UrlResource("file:core/io/ResourceTests.class");
			assertThat(new UrlResource("file:core/../core/io/./ResourceTests.class")).isEqualTo(resource);
		}

		@Test
		void filenameIsExtractedFromFilePath() throws Exception {
			assertThat(new UrlResource("file:/dir/test.txt?argh").getFilename()).isEqualTo("test.txt");
			assertThat(new UrlResource("file:\\dir\\test.txt?argh").getFilename()).isEqualTo("test.txt");
			assertThat(new UrlResource("file:\\dir/test.txt?argh").getFilename()).isEqualTo("test.txt");
		}

		@Test
		void filenameContainingHashTagIsExtractedFromFilePathUnencoded() throws Exception {
			String unencodedPath = "/dir/test#1.txt";
			String encodedPath = "/dir/test%231.txt";

			URI uri = new URI("file", unencodedPath, null);
			URL url = uri.toURL();
			assertThat(uri.getPath()).isEqualTo(unencodedPath);
			assertThat(uri.getRawPath()).isEqualTo(encodedPath);
			assertThat(url.getPath()).isEqualTo(encodedPath);

			UrlResource urlResource = new UrlResource(url);
			assertThat(urlResource.getURI().getPath()).isEqualTo(unencodedPath);
			assertThat(urlResource.getFilename()).isEqualTo("test#1.txt");
		}

		@Test
		void factoryMethodsProduceEqualResources() throws Exception {
			Resource resource1 = new UrlResource("file:core/io/ResourceTests.class");
			Resource resource2 = UrlResource.from("file:core/io/ResourceTests.class");
			Resource resource3 = UrlResource.from(resource1.getURI());

			assertThat(resource2.getURL()).isEqualTo(resource1.getURL());
			assertThat(resource3.getURL()).isEqualTo(resource1.getURL());

			assertThat(UrlResource.from("file:core/../core/io/./ResourceTests.class")).isEqualTo(resource1);
			assertThat(UrlResource.from("file:/dir/test.txt?argh").getFilename()).isEqualTo("test.txt");
			assertThat(UrlResource.from("file:\\dir\\test.txt?argh").getFilename()).isEqualTo("test.txt");
			assertThat(UrlResource.from("file:\\dir/test.txt?argh").getFilename()).isEqualTo("test.txt");
		}

		@Test
		void relativeResourcesAreEqual() throws Exception {
			Resource resource = new UrlResource("file:dir/");
			Resource relative = resource.createRelative("subdir");
			assertThat(relative).isEqualTo(new UrlResource("file:dir/subdir"));
		}

		@Test
		void missingRemoteResourceDoesNotExist() throws Exception {
			String baseUrl = startServer();
			UrlResource resource = new UrlResource(baseUrl + "/missing");
			assertThat(resource.exists()).isFalse();
		}

		@Test
		void remoteResourceExists() throws Exception {
			String baseUrl = startServer();
			UrlResource resource = new UrlResource(baseUrl + "/resource");
			assertThat(resource.exists()).isTrue();
			assertThat(resource.contentLength()).isEqualTo(6);
		}

		@Test
		void canCustomizeHttpUrlConnectionForExists() throws Exception {
			String baseUrl = startServer();
			CustomResource resource = new CustomResource(baseUrl + "/resource");
			assertThat(resource.exists()).isTrue();
			RecordedRequest request = this.server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("HEAD");
			assertThat(request.getHeader("Framework-Name")).isEqualTo("Spring");
		}

		@Test
		void canCustomizeHttpUrlConnectionForRead() throws Exception {
			String baseUrl = startServer();
			CustomResource resource = new CustomResource(baseUrl + "/resource");
			assertThat(resource.getInputStream()).hasContent("Spring");
			RecordedRequest request = this.server.takeRequest();
			assertThat(request.getMethod()).isEqualTo("GET");
			assertThat(request.getHeader("Framework-Name")).isEqualTo("Spring");
		}

		@AfterEach
		void shutdown() throws Exception {
			this.server.shutdown();
		}

		private String startServer() throws Exception {
			this.server.setDispatcher(new ResourceDispatcher());
			this.server.start();
			return "http://localhost:" + this.server.getPort();
		}

		class CustomResource extends UrlResource {

			public CustomResource(String path) throws MalformedURLException {
				super(path);
			}

			@Override
			protected void customizeConnection(HttpURLConnection con) throws IOException {
				con.setRequestProperty("Framework-Name", "Spring");
			}
		}

		class ResourceDispatcher extends Dispatcher {

			@Override
			public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
				if (request.getPath().equals("/resource")) {
					switch (request.getMethod()) {
						case "HEAD":
							return new MockResponse()
									.addHeader("Content-Length", "6");
						case "GET":
							return new MockResponse()
									.addHeader("Content-Length", "6")
									.addHeader("Content-Type", "text/plain")
									.setBody("Spring");
					}
				}
				return new MockResponse().setResponseCode(404);
			}
		}
	}

	@Nested
	class AbstractResourceTests {

		@Test
		void missingResourceIsNotReadable() {
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

			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getURL)
					.withMessageContaining(name);
			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getFile)
					.withMessageContaining(name);
			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(() ->
					resource.createRelative("/testing")).withMessageContaining(name);
			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(resource::getContentAsByteArray);
			assertThatExceptionOfType(FileNotFoundException.class).isThrownBy(
					() -> resource.getContentAsString(StandardCharsets.US_ASCII));
			assertThat(resource.getFilename()).isNull();
		}

		@Test
		void hasContentLength() throws Exception {
			AbstractResource resource = new AbstractResource() {
				@Override
				public InputStream getInputStream() {
					return new ByteArrayInputStream(new byte[] {'a', 'b', 'c'});
				}
				@Override
				public String getDescription() {
					return "";
				}
			};
			assertThat(resource.contentLength()).isEqualTo(3L);
		}

	}

}
