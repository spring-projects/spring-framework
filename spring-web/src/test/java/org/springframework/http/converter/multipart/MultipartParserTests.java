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

package org.springframework.http.converter.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultipartParser}.
 *
 * @author Brian Clozel
 * @author Arjen Poutsma
 */
class MultipartParserTests {

	private static final MediaType TEXT_PLAIN_ASCII = new MediaType("text", "plain", StandardCharsets.US_ASCII);

	@Test
	void simple() throws Exception {
		TestListener listener = new TestListener();
		parse("simple.multipart", "simple-boundary", listener);

		listener.assertHeader(headers -> assertThat(headers.isEmpty()).isTrue())
				.assertBodyChunk("This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak.")
				.assertHeader(headers -> assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN_ASCII))
				.assertBodyChunk("This is explicitly typed plain ASCII text.\r\nIt DOES end with a linebreak.\r\n")
				.assertComplete();
	}

	@Test
	void noHeaders() throws Exception {
		TestListener listener = new TestListener();
		parse("no-header.multipart", "boundary", listener);

		listener.assertHeader(headers -> assertThat(headers.isEmpty()).isTrue())
				.assertBodyChunk("a")
				.assertComplete();
	}

	@Test
	void noEndBoundary() throws Exception {
		TestListener listener = new TestListener();
		parse("no-end-boundary.multipart", "boundary", listener);

		assertThat(listener.error).isInstanceOf(HttpMessageConversionException.class);
	}

	@Test
	void garbage() throws Exception {
		TestListener listener = new TestListener();
		parse("garbage-1.multipart", "boundary", listener);

		assertThat(listener.error).isInstanceOf(HttpMessageConversionException.class);
	}

	@Test
	void noEndHeader() throws Exception {
		TestListener listener = new TestListener();
		parse("no-end-header.multipart", "boundary", listener);

		assertThat(listener.error).isInstanceOf(HttpMessageConversionException.class);
	}

	@Test
	void noEndBody() throws Exception {
		TestListener listener = new TestListener();
		parse("no-end-body.multipart", "boundary", listener);

		assertThat(listener.error).isInstanceOf(HttpMessageConversionException.class);
	}

	@Test
	void noBody() throws Exception {
		TestListener listener = new TestListener();
		parse("no-body.multipart", "boundary", listener);

		listener.assertHeader(headers -> assertThat(headers.hasHeaderValues("Part", List.of("1"))).isTrue())
				.assertHeader(headers -> assertThat(headers.hasHeaderValues("Part", List.of("2"))).isTrue())
				.assertBodyChunk("a")
				.assertComplete();
	}

	@Test
	void firefox() throws Exception {
		TestListener listener = new TestListener();
		parse("firefox.multipart",
				"---------------------------18399284482060392383840973206", listener);

		listener.assertHeadersFormField("text1")
				.assertBodyChunk("a")
				.assertHeadersFormField("text2")
				.assertBodyChunk("b")
				.assertHeadersFile("file1", "a.txt")
				.assertBodyChunk()
				.assertHeadersFile("file2", "a.txt")
				.assertBodyChunk()
				.assertHeadersFile("file2", "b.txt")
				.assertBodyChunk()
				.assertComplete();
	}

	@Test
	void chrome() throws Exception {
		TestListener listener = new TestListener();
		parse("chrome.multipart",
				"----WebKitFormBoundaryEveBLvRT65n21fwU", listener);

		listener.assertHeadersFormField("text1")
				.assertBodyChunk("a")
				.assertHeadersFormField("text2")
				.assertBodyChunk("b")
				.assertHeadersFile("file1", "a.txt")
				.assertBodyChunk()
				.assertHeadersFile("file2", "a.txt")
				.assertBodyChunk()
				.assertHeadersFile("file2", "b.txt")
				.assertBodyChunk()
				.assertComplete();
	}

	@Test
	void safari() throws Exception {
		TestListener listener = new TestListener();
		parse("safari.multipart",
				"----WebKitFormBoundaryG8fJ50opQOML0oGD", listener);

		listener.assertHeadersFormField("text1")
				.assertBodyChunk("a")
				.assertHeadersFormField("text2")
				.assertBodyChunk("b")
				.assertHeadersFile("file1", "a.txt")
				.assertBodyChunk()
				.assertHeadersFile("file2", "a.txt")
				.assertBodyChunk()
				.assertHeadersFile("file2", "b.txt")
				.assertBodyChunk()
				.assertComplete();
	}

	@Test
	void utf8Headers() throws Exception {
		TestListener listener = new TestListener();
		parse("utf8.multipart", "simple-boundary", listener);

		listener.assertHeader(headers ->
						assertThat(headers.hasHeaderValues("Føø", List.of("Bår"))).isTrue())
				.assertBodyChunk("This is plain ASCII text.")
				.assertComplete();
	}

	private InputStream createStream(String fileName) throws IOException {
		Resource resource = new ClassPathResource("/org/springframework/http/multipart/" + fileName);
		return resource.getInputStream();
	}

	private void parse(String fileName, String boundary, MultipartParser.PartListener listener) throws Exception {
		try (InputStream input = createStream(fileName)) {
			MultipartParser multipartParser = new MultipartParser(10 * 1024, 4 * 1024);
			multipartParser.parse(input, boundary.getBytes(UTF_8), StandardCharsets.UTF_8, listener);
		}
	}


	static class TestListener implements MultipartParser.PartListener {

		Deque<Object> received = new ArrayDeque<>();

		boolean complete;

		Throwable error;

		@Override
		public void onHeaders(@NonNull HttpHeaders headers) {
			this.received.add(headers);
		}

		@Override
		public void onBody(@NonNull DataBuffer buffer, boolean last) {
			this.received.add(buffer);
		}

		@Override
		public void onComplete() {
			this.complete = true;
		}

		@Override
		public void onError(@NonNull Throwable error) {
			this.error = error;
		}

		TestListener assertHeader(Consumer<HttpHeaders> headersConsumer) {
			Object value = received.pollFirst();
			assertThat(value).isInstanceOf(HttpHeaders.class);
			headersConsumer.accept((HttpHeaders) value);
			return this;
		}

		TestListener assertHeadersFormField(String expectedName) {
			return assertHeader(headers -> {
				ContentDisposition cd = headers.getContentDisposition();
				assertThat(cd.isFormData()).isTrue();
				assertThat(cd.getName()).isEqualTo(expectedName);
			});
		}

		TestListener assertHeadersFile(String expectedName, String expectedFilename) {
			return assertHeader(headers -> {
				ContentDisposition cd = headers.getContentDisposition();
				assertThat(cd.isFormData()).isTrue();
				assertThat(cd.getName()).isEqualTo(expectedName);
				assertThat(cd.getFilename()).isEqualTo(expectedFilename);
			});
		}

		TestListener assertBodyChunk(Consumer<DataBuffer> bodyConsumer) {
			Object value = received.pollFirst();
			assertThat(value).isInstanceOf(DataBuffer.class);
			bodyConsumer.accept((DataBuffer) value);
			DataBufferUtils.release((DataBuffer) value);
			return this;
		}

		TestListener assertBodyChunk(String bodyContent) {
			return assertBodyChunk(buffer -> {
				String actual = buffer.toString(UTF_8);
				assertThat(actual).isEqualTo(bodyContent);
			});
		}

		TestListener assertBodyChunk() {
			return assertBodyChunk(buffer -> {
			});
		}

		TestListener assertLastBodyChunk() {
			if (!received.isEmpty()) {
				assertThat(received.peek()).isNotInstanceOf(DataBuffer.class);
			}
			return this;
		}

		void assertComplete() {
			assertThat(this.complete).isTrue();
		}
	}

}
