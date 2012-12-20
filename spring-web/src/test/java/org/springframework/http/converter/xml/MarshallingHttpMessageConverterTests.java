/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.http.converter.xml;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

/** @author Arjen Poutsma */
public class MarshallingHttpMessageConverterTests {

	private MarshallingHttpMessageConverter converter;

	private Marshaller marshaller;

	private Unmarshaller unmarshaller;

	@Before
	public void setUp() {
		marshaller = createMock(Marshaller.class);
		unmarshaller = createMock(Unmarshaller.class);

		converter = new MarshallingHttpMessageConverter(marshaller, unmarshaller);
	}

	@Test
	public void read() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));

		expect(unmarshaller.unmarshal(isA(StreamSource.class))).andReturn(body);

		replay(marshaller, unmarshaller);
		String result = (String) converter.read(Object.class, inputMessage);
		assertEquals("Invalid result", body, result);
		verify(marshaller, unmarshaller);
	}

	@Test
	public void write() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		marshaller.marshal(eq(body), isA(StreamResult.class));

		replay(marshaller, unmarshaller);
		converter.write(body, null, outputMessage);
		assertEquals("Invalid content-type", new MediaType("application", "xml"),
				outputMessage.getHeaders().getContentType());
		verify(marshaller, unmarshaller);
	}
}
