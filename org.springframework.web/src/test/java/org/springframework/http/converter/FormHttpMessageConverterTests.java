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
import java.util.List;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/** @author Arjen Poutsma */
public class FormHttpMessageConverterTests {

	private FormHttpMessageConverter converter;

	@Before
	public void setUp() {
		converter = new FormHttpMessageConverter();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void read() throws Exception {
		String body = "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3";
		Charset iso88591 = Charset.forName("ISO-8859-1");
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes(iso88591));
		inputMessage.getHeaders().setContentType(new MediaType("application", "x-www-form-urlencoded", iso88591));
		MultiValueMap result = converter.read(null, inputMessage);
		assertEquals("Invalid result", 3, result.size());
		assertEquals("Invalid result", "value 1", result.getFirst("name 1"));
		List<String> values = (List<String>) result.get("name 2");
		assertEquals("Invalid result", 2, values.size());
		assertEquals("Invalid result", "value 2+1", values.get(0));
		assertEquals("Invalid result", "value 2+2", values.get(1));
		assertNull("Invalid result", result.getFirst("name 3"));
	}

	@Test
	public void write() throws IOException {
		MultiValueMap<String, String> body = new LinkedMultiValueMap<String, String>();
		body.set("name 1", "value 1");
		body.add("name 2", "value 2+1");
		body.add("name 2", "value 2+2");
		body.add("name 3", null);
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		converter.write(body, null, outputMessage);
		Charset iso88591 = Charset.forName("ISO-8859-1");
		assertEquals("Invalid result", "name+1=value+1&name+2=value+2%2B1&name+2=value+2%2B2&name+3",
				outputMessage.getBodyAsString(iso88591));
		assertEquals("Invalid content-type", new MediaType("application", "x-www-form-urlencoded"),
				outputMessage.getHeaders().getContentType());
	}

}
