/*
 * Copyright 2002-2024 the original author or authors.
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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;

import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.entry;
import static org.springframework.core.ResolvableType.forClass;

/**
 * @author Arjen Poutsma
 */
class PartEventHttpMessageReaderTests {

	private static final int BUFFER_SIZE = 64;

	private static final DataBufferFactory bufferFactory = new NettyDataBufferFactory(new PooledByteBufAllocator());

	private static final MediaType TEXT_PLAIN_ASCII = new MediaType("text", "plain", StandardCharsets.US_ASCII);

	private final PartEventHttpMessageReader reader = new PartEventHttpMessageReader();

	@Test
	void canRead() {
		assertThat(this.reader.canRead(forClass(PartEvent.class), MediaType.MULTIPART_FORM_DATA)).isTrue();
		assertThat(this.reader.canRead(forClass(PartEvent.class), null)).isTrue();
	}

	@Test
	void simple() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("simple.multipart", getClass()), "simple-boundary");

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.assertNext(form(headers -> assertThat(headers).isEmpty(), "This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak."))
				.assertNext(form(headers -> assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN_ASCII),
						"This is explicitly typed plain ASCII text.\r\nIt DOES end with a linebreak.\r\n"))
				.verifyComplete();
	}

	@Test
	void noHeaders() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("no-header.multipart", getClass()), "boundary");
		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.assertNext(data(headers -> assertThat(headers).isEmpty(), bodyText("a"), true))
				.verifyComplete();
	}

	@Test
	void noEndBoundary() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("no-end-boundary.multipart", getClass()), "boundary");

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.expectError(DecodingException.class)
				.verify();
	}

	@Test
	void garbage() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("garbage-1.multipart", getClass()), "boundary");

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.expectError(DecodingException.class)
				.verify();
	}


	@Test
	void noEndHeader() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("no-end-header.multipart", getClass()), "boundary");
		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.expectError(DecodingException.class)
				.verify();
	}

	@Test
	void noEndBody() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("no-end-body.multipart", getClass()), "boundary");
		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.expectError(DecodingException.class)
				.verify();
	}

	@Test
	void noBody() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("no-body.multipart", getClass()), "boundary");
		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.assertNext(form(headers -> assertThat(headers).contains(entry("Part", List.of("1"))), ""))
				.assertNext(data(headers -> assertThat(headers).contains(entry("Part", List.of("2"))), bodyText("a"), true))
				.verifyComplete();
	}


	@Test
	void cancel() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("simple.multipart", getClass()), "simple-boundary");
		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result, 3)
				.assertNext(form(headers -> assertThat(headers).isEmpty(),
						"This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak."))
				.thenCancel()
				.verify();
	}


	@Test
	void firefox() {

		MockServerHttpRequest request = createRequest(new ClassPathResource("firefox.multipart", getClass()),
				"---------------------------18399284482060392383840973206");

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());
		StepVerifier.create(result)
				.assertNext(data(headersFormField("text1"), bodyText("a"), true))
				.assertNext(data(headersFormField("text2"), bodyText("b"), true))
				.assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, true))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
				.verifyComplete();
	}

	@Test
	void chrome() {

		MockServerHttpRequest request = createRequest(new ClassPathResource("chrome.multipart", getClass()),
				"----WebKitFormBoundaryEveBLvRT65n21fwU");

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());
		StepVerifier.create(result)
				.assertNext(data(headersFormField("text1"), bodyText("a"), true))
				.assertNext(data(headersFormField("text2"), bodyText("b"), true))
				.assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, true))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
				.verifyComplete();
	}

	@Test
	void safari() {

		MockServerHttpRequest request = createRequest(new ClassPathResource("safari.multipart", getClass()),
				"----WebKitFormBoundaryG8fJ50opQOML0oGD");

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());
		StepVerifier.create(result)
				.assertNext(data(headersFormField("text1"), bodyText("a"), true))
				.assertNext(data(headersFormField("text2"), bodyText("b"), true))
				.assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file1", "a.txt"), DataBufferUtils::release, true))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, false))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
				.verifyComplete();
	}

	@Test
	void tooManyParts() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("simple.multipart", getClass()), "simple-boundary");

		PartEventHttpMessageReader reader = new PartEventHttpMessageReader();
		reader.setMaxParts(1);

		Flux<PartEvent> result = reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.assertNext(form(headers -> assertThat(headers).isEmpty(), "This is implicitly typed plain ASCII text.\r\nIt does NOT end with a linebreak."))
				.expectError(DecodingException.class)
				.verify();
	}

	@Test
	void partSizeTooLarge() {
		MockServerHttpRequest request = createRequest(new ClassPathResource("safari.multipart", getClass()),
				"----WebKitFormBoundaryG8fJ50opQOML0oGD");

		PartEventHttpMessageReader reader = new PartEventHttpMessageReader();
		reader.setMaxPartSize(60);

		Flux<PartEvent> result = reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.assertNext(data(headersFormField("text1"), bodyText("a"), true))
				.assertNext(data(headersFormField("text2"), bodyText("b"), true))
				.expectError(DataBufferLimitException.class)
				.verify();
	}

	@Test
	void formPartTooLarge() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("simple.multipart", getClass()), "simple-boundary");

		PartEventHttpMessageReader reader = new PartEventHttpMessageReader();
		reader.setMaxInMemorySize(40);

		Flux<PartEvent> result = reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.expectError(DataBufferLimitException.class)
				.verify();
	}

	@Test
	void utf8Headers() {
		MockServerHttpRequest request = createRequest(
				new ClassPathResource("utf8.multipart", getClass()), "\"simple-boundary\"");

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.assertNext(data(headers -> assertThat(headers).containsEntry("Føø", List.of("Bår")),
						bodyText("This is plain ASCII text."), true))
				.verifyComplete();
	}

	@Test
	void exceedHeaderLimit() {
		Flux<DataBuffer> body = DataBufferUtils
				.readByteChannel((new ClassPathResource("files.multipart", getClass()))::readableChannel, bufferFactory,
						282);

		MediaType contentType = new MediaType("multipart", "form-data",
				singletonMap("boundary", "----WebKitFormBoundaryG8fJ50opQOML0oGD"));
		MockServerHttpRequest request = MockServerHttpRequest.post("/")
				.contentType(contentType)
				.body(body);

		this.reader.setMaxHeadersSize(230);

		Flux<PartEvent> result = this.reader.read(forClass(PartEvent.class), request, emptyMap());

		StepVerifier.create(result)
				.assertNext(data(headersFile("file2", "a.txt"), DataBufferUtils::release, true))
				.assertNext(data(headersFile("file2", "b.txt"), DataBufferUtils::release, true))
				.verifyComplete();
	}

	private MockServerHttpRequest createRequest(Resource resource, String boundary) {
		Flux<DataBuffer> body = DataBufferUtils
				.readByteChannel(resource::readableChannel, bufferFactory, BUFFER_SIZE);

		MediaType contentType = new MediaType("multipart", "form-data", singletonMap("boundary", boundary));
		return MockServerHttpRequest.post("/")
				.contentType(contentType)
				.body(body);
	}

	private static Consumer<PartEvent> form(Consumer<HttpHeaders> headersConsumer, String value) {
		return data -> {
			headersConsumer.accept(data.headers());
			String actual = data.content().toString(UTF_8);
			assertThat(actual).isEqualTo(value);
			assertThat(data.isLast()).isTrue();
		};
	}

	private static Consumer<PartEvent> data(Consumer<HttpHeaders> headersConsumer, Consumer<DataBuffer> bufferConsumer, boolean isLast) {
		return data -> {
			headersConsumer.accept(data.headers());
			bufferConsumer.accept(data.content());
			assertThat(data.isLast()).isEqualTo(isLast);
		};
	}

	private static Consumer<HttpHeaders> headersFormField(String expectedName) {
		return headers -> {
			ContentDisposition cd = headers.getContentDisposition();
			assertThat(cd.isFormData()).isTrue();
			assertThat(cd.getName()).isEqualTo(expectedName);
		};
	}

	private static Consumer<HttpHeaders> headersFile(String expectedName, String expectedFilename) {
		return headers -> {
			ContentDisposition cd = headers.getContentDisposition();
			assertThat(cd.isFormData()).isTrue();
			assertThat(cd.getName()).isEqualTo(expectedName);
			assertThat(cd.getFilename()).isEqualTo(expectedFilename);
		};
	}

	private static Consumer<DataBuffer> bodyText(String expected) {
		return buffer -> {
			String s = buffer.toString(UTF_8);
			DataBufferUtils.release(buffer);
			assertThat(s).isEqualTo(expected);
		};
	}

}
