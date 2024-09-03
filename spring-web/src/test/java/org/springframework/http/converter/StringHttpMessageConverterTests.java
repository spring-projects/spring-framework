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

package org.springframework.http.converter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link StringHttpMessageConverter}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
class StringHttpMessageConverterTests {

	private static final MediaType TEXT_PLAIN_UTF_8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

	private final StringHttpMessageConverter converter = new StringHttpMessageConverter();

	private final MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();


	@Test
	void canRead() {
		assertThat(this.converter.canRead(String.class, MediaType.TEXT_PLAIN)).isTrue();
	}

	@Test
	void canWrite() {
		assertThat(this.converter.canWrite(String.class, MediaType.TEXT_PLAIN)).isTrue();
		assertThat(this.converter.canWrite(String.class, MediaType.ALL)).isTrue();
	}

	@Test
	void read() throws IOException {
		String body = "Hello World";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(TEXT_PLAIN_UTF_8);
		String result = this.converter.read(String.class, inputMessage);

		assertThat(result).as("Invalid result").isEqualTo(body);
	}

	@Test
	void readWithContentLengthHeader() throws IOException {
		String body = "Hello World";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentLength(body.length());
		inputMessage.getHeaders().setContentType(TEXT_PLAIN_UTF_8);
		String result = this.converter.read(String.class, inputMessage);

		assertThat(result).as("Invalid result").isEqualTo(body);
	}

	@Test // gh-24123
	void readJson() throws IOException {
		String body = "{\"result\":\"\u0414\u0410\"}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);
		String result = this.converter.read(String.class, inputMessage);

		assertThat(result).as("Invalid result").isEqualTo(body);
	}

	@Test // gh-25328
	void readJsonApi() throws IOException {
		String body = "{\"result\":\"\u0414\u0410\"}";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(new MediaType("application", "vnd.api.v1+json"));
		String result = this.converter.read(String.class, inputMessage);

		assertThat(result).as("Invalid result").isEqualTo(body);
	}

	@Test
	void writeDefaultCharset() throws IOException {
		String body = "H\u00e9llo W\u00f6rld";
		this.converter.write(body, null, this.outputMessage);

		HttpHeaders headers = this.outputMessage.getHeaders();
		assertThat(this.outputMessage.getBodyAsString(StandardCharsets.ISO_8859_1)).isEqualTo(body);
		assertThat(headers.getContentType()).isEqualTo(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
		assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.ISO_8859_1).length);
		assertThat(headers.getAcceptCharset()).isEmpty();
	}

	@Test  // gh-24123
	void writeJson() throws IOException {
		String body = "{\"føø\":\"bår\"}";
		this.converter.write(body, MediaType.APPLICATION_JSON, this.outputMessage);

		HttpHeaders headers = this.outputMessage.getHeaders();
		assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
		assertThat(headers.getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
		assertThat(headers.getAcceptCharset()).isEmpty();
	}

	@Test  // gh-25328
	void writeJsonApi() throws IOException {
		String body = "{\"føø\":\"bår\"}";
		MediaType contentType = new MediaType("application", "vnd.api.v1+json");
		this.converter.write(body, contentType, this.outputMessage);

		HttpHeaders headers = this.outputMessage.getHeaders();
		assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
		assertThat(headers.getContentType()).isEqualTo(contentType);
		assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
		assertThat(headers.getAcceptCharset()).isEmpty();
	}

	@Test
	void writeUTF8() throws IOException {
		String body = "H\u00e9llo W\u00f6rld";
		this.converter.write(body, TEXT_PLAIN_UTF_8, this.outputMessage);

		HttpHeaders headers = this.outputMessage.getHeaders();
		assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
		assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN_UTF_8);
		assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
		assertThat(headers.getAcceptCharset()).isEmpty();
	}

	@Test  // SPR-8867
	void writeOverrideRequestedContentType() throws IOException {
		String body = "H\u00e9llo W\u00f6rld";
		MediaType requestedContentType = new MediaType("text", "html");

		HttpHeaders headers = this.outputMessage.getHeaders();
		headers.setContentType(TEXT_PLAIN_UTF_8);
		this.converter.write(body, requestedContentType, this.outputMessage);

		assertThat(this.outputMessage.getBodyAsString(StandardCharsets.UTF_8)).isEqualTo(body);
		assertThat(headers.getContentType()).isEqualTo(TEXT_PLAIN_UTF_8);
		assertThat(headers.getContentLength()).isEqualTo(body.getBytes(StandardCharsets.UTF_8).length);
		assertThat(headers.getAcceptCharset()).isEmpty();
	}

	@Test // gh-24283
	void writeWithWildCardMediaType() throws IOException {
		String body = "Hello World";
		this.converter.write(body, MediaType.ALL, this.outputMessage);

		HttpHeaders headers = this.outputMessage.getHeaders();
		assertThat(this.outputMessage.getBodyAsString(StandardCharsets.US_ASCII)).isEqualTo(body);
		assertThat(headers.getContentType()).isEqualTo(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
		assertThat(headers.getContentLength()).isEqualTo(body.getBytes().length);
		assertThat(headers.getAcceptCharset()).isEmpty();
	}

}
