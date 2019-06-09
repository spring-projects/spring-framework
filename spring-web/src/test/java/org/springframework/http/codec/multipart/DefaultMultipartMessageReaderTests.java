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

package org.springframework.http.codec.multipart;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.AbstractDataBufferAllocatingTestCase;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.ResolvableType.forClass;

/**
 * @author Arjen Poutsma
 */
public class DefaultMultipartMessageReaderTests extends AbstractDataBufferAllocatingTestCase {

	private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer iaculis metus id vestibulum nullam.";

	private static final String MUSPI_MEROL = new StringBuilder(LOREM_IPSUM).reverse().toString();

	private static final int BUFFER_SIZE = 16;

	private final DefaultMultipartMessageReader reader = new DefaultMultipartMessageReader();

	@Test
	public void canRead() {
		assertThat(this.reader.canRead(forClass(Part.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
	}

	@Test
	public void partNoHeader() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("part-no-header.multipart", getClass()), "boundary");

		Flux<Part> result = this.reader.read(forClass(Part.class), request, emptyMap());

		StepVerifier.create(result)
				.consumeNextWith(part -> {
					assertThat(part.headers().isEmpty()).isTrue();
					part.content().subscribe(DataBufferUtils::release);
				})
				.verifyComplete();
	}

	@Test
	public void partNoEndBoundary() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("part-no-end-boundary.multipart", getClass()), "boundary");

		Flux<Part> result = this.reader.read(forClass(Part.class), request, emptyMap());

		StepVerifier.create(result)
				.consumeNextWith(part ->
					part.content().subscribe(DataBufferUtils::release)
				)
				.verifyComplete();
	}

	@Test
	public void firefox() {
		testBrowser(new ClassPathResource("firefox.multipart", getClass()),
				"---------------------------18399284482060392383840973206");
	}

	@Test
	public void chrome() {
		testBrowser(new ClassPathResource("chrome.multipart", getClass()),
				"----WebKitFormBoundaryEveBLvRT65n21fwU");
	}

	@Test
	public void safari() {
		testBrowser(new ClassPathResource("safari.multipart", getClass()),
				"----WebKitFormBoundaryG8fJ50opQOML0oGD");
	}

	private void testBrowser(Resource resource, String boundary) {
		MockServerHttpRequest request = createRequest(resource, boundary);

		Flux<Part> result = this.reader.read(forClass(Part.class), request, emptyMap());

		StepVerifier.create(result)
				.consumeNextWith(part -> testBrowserFormField(part, "text1", "a"))
				.consumeNextWith(part -> testBrowserFormField(part, "text2", "b"))
				.consumeNextWith(part -> testBrowserFile(part, "file1", "a.txt", LOREM_IPSUM))
				.consumeNextWith(part -> testBrowserFile(part, "file2", "a.txt", LOREM_IPSUM))
				.consumeNextWith(part -> testBrowserFile(part, "file2", "b.txt", MUSPI_MEROL))
				.verifyComplete();
	}

	private MockServerHttpRequest createRequest(Resource resource, String boundary) {
		Flux<DataBuffer> body = DataBufferUtils
				.readByteChannel(resource::readableChannel, this.bufferFactory, BUFFER_SIZE);

		MediaType contentType = new MediaType("multipart", "form-data", singletonMap("boundary", boundary));
		return MockServerHttpRequest.post("/")
				.contentType(contentType)
				.body(body);
	}

	private static void testBrowserFormField(Part part, String name, String value) {
		boolean condition = part instanceof FormFieldPart;
		assertThat(condition).isTrue();
		assertThat(part.name()).isEqualTo(name);
		FormFieldPart formField = (FormFieldPart) part;
		assertThat(formField.value()).isEqualTo(value);
	}

	private static void testBrowserFile(Part part, String name, String filename, String contents) {
		try {
			boolean condition = part instanceof FilePart;
			assertThat(condition).isTrue();
			assertThat(part.name()).isEqualTo(name);
			FilePart file = (FilePart) part;
			assertThat(file.filename()).isEqualTo(filename);

			Path tempFile = Files.createTempFile("DefaultMultipartMessageReaderTests", null);

			CountDownLatch latch = new CountDownLatch(1);
			file.transferTo(tempFile)
					.subscribe(null,
							throwable -> {
								throw new AssertionError(throwable.getMessage(), throwable);
							},
							() -> {
								try {
									verifyContents(tempFile, contents);
								}
								finally {
									latch.countDown();
								}

							});

			latch.await();
		}
		catch (Exception ex) {
			throw new AssertionError(ex);
		}
	}

	private static void verifyContents(Path tempFile, String contents) {
		try {
			String result = String.join("", Files.readAllLines(tempFile));
			assertThat(result).isEqualTo(contents);
		}
		catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}

}
