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

package org.springframework.web.converter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.util.MediaType;
import org.springframework.web.http.MockHttpInputMessage;
import org.springframework.web.http.MockHttpOutputMessage;

/**
 * @author Arjen Poutsma
 */
public class StringHttpMessageConverterTests {

	private StringHttpMessageConverter converter;

	@Before
	public void setUp() {
		converter = new StringHttpMessageConverter();
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
	public void write() throws IOException {
		Charset charset = Charset.forName("UTF-8");
		converter.setSupportedMediaTypes(Collections.singletonList(new MediaType("text", "plain", charset)));
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		String body = "Hello World";
		converter.write(body, outputMessage);
		assertEquals("Invalid result", body, outputMessage.getBodyAsString(charset));
		assertEquals("Invalid content-type", new MediaType("text", "plain", charset),
				outputMessage.getHeaders().getContentType());
		assertEquals("Invalid content-length", 11, outputMessage.getHeaders().getContentLength());
		assertFalse("Invalid accept-charset", outputMessage.getHeaders().getAcceptCharset().isEmpty());
	}
}
