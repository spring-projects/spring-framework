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

package org.springframework.http.converter;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.web.testfixture.http.MockHttpInputMessage;
import org.springframework.web.testfixture.http.MockHttpOutputMessage;

import static org.assertj.core.api.Assertions.assertThat;

/** @author Arjen Poutsma */
class ByteArrayHttpMessageConverterTests {

	private ByteArrayHttpMessageConverter converter;


	@BeforeEach
	void setUp() {
		converter = new ByteArrayHttpMessageConverter();
	}


	@Test
	void canRead() {
		assertThat(converter.canRead(byte[].class, new MediaType("application", "octet-stream"))).isTrue();
	}

	@Test
	void canWrite() {
		assertThat(converter.canWrite(byte[].class, new MediaType("application", "octet-stream"))).isTrue();
		assertThat(converter.canWrite(byte[].class, MediaType.ALL)).isTrue();
	}

	@Test
	void read() throws IOException {
		byte[] body = new byte[]{0x1, 0x2};
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(new MediaType("application", "octet-stream"));
		byte[] result = converter.read(byte[].class, inputMessage);
		assertThat(result).as("Invalid result").isEqualTo(body);
	}

	@Test
	void readWithContentLengthHeaderSet() throws IOException {
		byte[] body = new byte[]{0x1, 0x2, 0x3, 0x4, 0x5};
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body);
		inputMessage.getHeaders().setContentType(new MediaType("application", "octet-stream"));
		inputMessage.getHeaders().setContentLength(body.length);
		byte[] result = converter.read(byte[].class, inputMessage);
		assertThat(result).as("Invalid result").isEqualTo(body);
	}

	@Test
	void write() throws IOException {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		byte[] body = new byte[]{0x1, 0x2};
		converter.write(body, null, outputMessage);
		assertThat(outputMessage.getBodyAsBytes()).as("Invalid result").isEqualTo(body);
		assertThat(outputMessage.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
		assertThat(outputMessage.getHeaders().getContentLength()).isEqualTo(2);
	}

	@Test
	void repeatableWrites() throws IOException {
		MockHttpOutputMessage outputMessage1 = new MockHttpOutputMessage();
		byte[] body = new byte[]{0x1, 0x2};
		assertThat(converter.supportsRepeatableWrites(body)).isTrue();

		converter.write(body, null, outputMessage1);
		assertThat(outputMessage1.getBodyAsBytes()).isEqualTo(body);

		MockHttpOutputMessage outputMessage2 = new MockHttpOutputMessage();
		converter.write(body, null, outputMessage2);
		assertThat(outputMessage2.getBodyAsBytes()).isEqualTo(body);
	}

}
