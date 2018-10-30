/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 */
public class StringHttpMessageConverterTests {

	public static final MediaType TEXT_PLAIN_UTF_8 = new MediaType("text", "plain", StandardCharsets.UTF_8);

	private StringHttpMessageConverter converter;

	private MockHttpOutputMessage outputMessage;


	@Before
	public void setUp() {
		this.converter = new StringHttpMessageConverter();
		this.outputMessage = new MockHttpOutputMessage();
	}


	@Test
	public void canRead() {
		assertTrue(this.converter.canRead(String.class, MediaType.TEXT_PLAIN));
	}

	@Test
	public void canWrite() {
		assertTrue(this.converter.canWrite(String.class, MediaType.TEXT_PLAIN));
		assertTrue(this.converter.canWrite(String.class, MediaType.ALL));
	}

	@Test
	public void read() throws IOException {
		String body = "Hello World";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(StandardCharsets.UTF_8));
		inputMessage.getHeaders().setContentType(TEXT_PLAIN_UTF_8);
		String result = this.converter.read(String.class, inputMessage);

		assertEquals("Invalid result", body, result);
	}

	@Test
	public void writeDefaultCharset() throws IOException {
		String body = "H\u00e9llo W\u00f6rld";
		this.converter.write(body, null, this.outputMessage);

		HttpHeaders headers = this.outputMessage.getHeaders();
		assertEquals(body, this.outputMessage.getBodyAsString(StandardCharsets.ISO_8859_1));
		assertEquals(new MediaType("text", "plain", StandardCharsets.ISO_8859_1), headers.getContentType());
		assertEquals(body.getBytes(StandardCharsets.ISO_8859_1).length, headers.getContentLength());
		assertFalse(headers.getAcceptCharset().isEmpty());
	}

	@Test
	public void writeUTF8() throws IOException {
		String body = "H\u00e9llo W\u00f6rld";
		this.converter.write(body, TEXT_PLAIN_UTF_8, this.outputMessage);

		HttpHeaders headers = this.outputMessage.getHeaders();
		assertEquals(body, this.outputMessage.getBodyAsString(StandardCharsets.UTF_8));
		assertEquals(TEXT_PLAIN_UTF_8, headers.getContentType());
		assertEquals(body.getBytes(StandardCharsets.UTF_8).length, headers.getContentLength());
		assertFalse(headers.getAcceptCharset().isEmpty());
	}

	@Test  // SPR-8867
	public void writeOverrideRequestedContentType() throws IOException {
		String body = "H\u00e9llo W\u00f6rld";
		MediaType requestedContentType = new MediaType("text", "html");

		HttpHeaders headers = this.outputMessage.getHeaders();
		headers.setContentType(TEXT_PLAIN_UTF_8);
		this.converter.write(body, requestedContentType, this.outputMessage);

		assertEquals(body, this.outputMessage.getBodyAsString(StandardCharsets.UTF_8));
		assertEquals(TEXT_PLAIN_UTF_8, headers.getContentType());
		assertEquals(body.getBytes(StandardCharsets.UTF_8).length, headers.getContentLength());
		assertFalse(headers.getAcceptCharset().isEmpty());
	}

}
