/*
 * Copyright 2002-2013 the original author or authors.
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

import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.MediaType;
import org.springframework.http.MockHttpInputMessage;
import org.springframework.http.MockHttpOutputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.MarshallingFailureException;
import org.springframework.oxm.Unmarshaller;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * @author Arjen Poutsma
 */
public class MarshallingHttpMessageConverterTests {

	@Test
	public void read() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));

		Unmarshaller unmarshaller1 = mock(Unmarshaller.class);
		Unmarshaller unmarshaller2 = mock(Unmarshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();

		converter.setUnmarshallers(unmarshaller1, unmarshaller2);

		when(unmarshaller1.supports(Object.class)).thenReturn(false);
		when(unmarshaller2.supports(Object.class)).thenReturn(true);
		when(unmarshaller2.unmarshal(isA(StreamSource.class))).thenReturn(body);

		String result = (String) converter.read(Object.class, inputMessage);
		assertEquals("Invalid result", body, result);
	}

	@Test(expected = TypeMismatchException.class)
	public void readWithTypeMismatchException() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);

		Marshaller marshaller = mock(Marshaller.class);
		Unmarshaller unmarshaller = mock(Unmarshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller,
				unmarshaller);

		when(unmarshaller.supports(String.class)).thenReturn(true);
		when(unmarshaller.unmarshal(isA(StreamSource.class))).thenReturn(Integer.valueOf(3));

		converter.read(String.class, inputMessage);
	}

	@Test(expected = HttpMessageNotReadableException.class)
	public void readWithHttpMessageNotReadableException() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);

		Unmarshaller unmarshaller1 = mock(Unmarshaller.class);
		Unmarshaller unmarshaller2 = mock(Unmarshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();

		converter.setUnmarshallers(unmarshaller1, unmarshaller2);

		when(unmarshaller1.supports(Object.class)).thenReturn(false);
		when(unmarshaller1.supports(Object.class)).thenReturn(false);

		converter.read(Object.class, inputMessage);
	}

	@Test
	public void readWithMarshallingFailureException() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
		MarshallingFailureException ex = new MarshallingFailureException("unexpected");

		Unmarshaller unmarshaller = mock(Unmarshaller.class);
		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(
				unmarshaller);

		when(unmarshaller.supports(Object.class)).thenReturn(true);
		when(unmarshaller.unmarshal(isA(StreamSource.class))).thenThrow(ex);

		try {
			converter.read(Object.class, inputMessage);
			fail("HttpMessageNotReadableException should be thrown");
		}
		catch (HttpMessageNotReadableException e) {
			assertTrue("Invalid exception hierarchy", e.getCause() == ex);
		}
	}

	@Test
	public void write() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		Marshaller marshaller = mock(Marshaller.class);
		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller);

		when(marshaller.supports(String.class)).thenReturn(true);
		doNothing().when(marshaller).marshal(eq(body), isA(Result.class));

		converter.write(body, null, outputMessage);

		assertEquals("Invalid content-type", new MediaType("application", "xml"), outputMessage
				.getHeaders().getContentType());
	}

	@Test(expected = HttpMessageNotWritableException.class)
	public void writeWithHttpMessageNotWritableException() throws Exception {
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();

		Marshaller marshaller1 = mock(Marshaller.class);
		Marshaller marshaller2 = mock(Marshaller.class);
		Marshaller marshaller3 = mock(Marshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();

		converter.setMarshallers(marshaller1, marshaller2, marshaller3);

		when(marshaller1.supports(String.class)).thenReturn(false);
		when(marshaller2.supports(String.class)).thenReturn(false);
		when(marshaller3.supports(String.class)).thenReturn(false);

		converter.write("", null, outputMessage);
	}

	@Test
	public void writeWithMarshallingFailureException() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MarshallingFailureException ex = new MarshallingFailureException("unexpected");

		Marshaller marshaller = mock(Marshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller);

		when(marshaller.supports(String.class)).thenReturn(true);
		doThrow(ex).when(marshaller).marshal(eq(body), isA(Result.class));

		try {
			converter.write(body, null, outputMessage);
			fail("HttpMessageNotWritableException should be thrown");
		}
		catch (HttpMessageNotWritableException e) {
			assertTrue("Invalid exception hierarchy", e.getCause() == ex);
		}
	}
}
