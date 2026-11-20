/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
