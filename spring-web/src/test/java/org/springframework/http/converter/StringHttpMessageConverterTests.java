/*
 * Copyright 2002-2009 the original author or authors.
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
import java.nio.charset.Charset;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;

/** @author Arjen Poutsma */
public class StringHttpMessageConverterTests {

	private StringHttpMessageConverter converter;

	@Before
	public void setUp() {
		converter = new StringHttpMessageConverter();
	}

	@Test
	public void canRead() {
		assertTrue(converter.canRead(String.class, new MediaType("text", "plain")));
	}

	@Test
	public void canWrite() {
		assertTrue(converter.canWrite(String.class, new MediaType("text", "plain")));
		assertTrue(converter.canWrite(String.class, MediaType.ALL));
	}

	@Test
	public void read() throws IOException {
		String body = "Hello World";
		Charset charset = Charset.forName("UTF-8");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(charset));
		inputMessage.getHeaders().setContentType(new MediaType("text", "plain", charset));
		String result = converter.read(String.class, inputMessage);
		assertEquals("Invalid result", body, result);
	}

	@Test
	public void writeDefaultCharset() throws IOException {
		Charset iso88591 = Charset.forName("ISO-8859-1");
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		String body = "H\u00e9llo W\u00f6rld";
		converter.write(body, null, outputMessage);
		assertEquals("Invalid result", body, outputMessage.getBodyAsString(iso88591));
		assertEquals("Invalid content-type", new MediaType("text", "plain", iso88591),
				outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", body.getBytes(iso88591).length,
				outputMessage.getHeaders().getContentLength());
		assertFalse("Invalid accept-charset", outputMessage.getHeaders().getAcceptCharset().isEmpty());
	}

	@Test
	public void writeUTF8() throws IOException {
		Charset utf8 = Charset.forName("UTF-8");
		MediaType contentType = new MediaType("text", "plain", utf8);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		String body = "H\u00e9llo W\u00f6rld";
		converter.write(body, contentType, outputMessage);
		assertEquals("Invalid result", body, outputMessage.getBodyAsString(utf8));
		assertEquals("Invalid content-type", contentType, outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", body.getBytes(utf8).length,
				outputMessage.getHeaders().getContentLength());
		assertFalse("Invalid accept-charset", outputMessage.getHeaders().getAcceptCharset().isEmpty());
	}

	// SPR-8867

	@Test
	public void writeOverrideRequestedContentType() throws IOException {
		Charset utf8 = Charset.forName("UTF-8");
		MediaType requestedContentType = new MediaType("text", "html");
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MediaType contentType = new MediaType("text", "plain", utf8);
		outputMessage.getHeaders().setContentType(contentType);
		String body = "H\u00e9llo W\u00f6rld";
		converter.write(body, requestedContentType, outputMessage);
		assertEquals("Invalid result", body, outputMessage.getBodyAsString(utf8));
		assertEquals("Invalid content-type", contentType, outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", body.getBytes(utf8).length,
				outputMessage.getHeaders().getContentLength());
		assertFalse("Invalid accept-charset", outputMessage.getHeaders().getAcceptCharset().isEmpty());
	}
}
