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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.springframework.oxm.UnmarshallingFailureException;

/**
 * Tests for {@link MarshallingHttpMessageConverter}.
 * 
 * @author Arjen Poutsma
 */
public class MarshallingHttpMessageConverterTests {

	@Test
	public void canRead() throws Exception {
		Unmarshaller unmarshaller = mock(Unmarshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();

		converter.setUnmarshaller(unmarshaller);

		when(unmarshaller.supports(Integer.class)).thenReturn(false);
		when(unmarshaller.supports(String.class)).thenReturn(true);

		assertFalse(converter.canRead(Boolean.class, MediaType.TEXT_PLAIN));
		assertFalse(converter.canRead(Integer.class, MediaType.TEXT_XML));
		assertTrue(converter.canRead(String.class, MediaType.TEXT_XML));
	}

	@Test
	public void canWrite() throws Exception {
		Marshaller marshaller = mock(Marshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();

		converter.setMarshaller(marshaller);

		when(marshaller.supports(Integer.class)).thenReturn(false);
		when(marshaller.supports(String.class)).thenReturn(true);

		assertFalse(converter.canWrite(Boolean.class, MediaType.TEXT_PLAIN));
		assertFalse(converter.canWrite(Integer.class, MediaType.TEXT_XML));
		assertTrue(converter.canWrite(String.class, MediaType.TEXT_XML));
	}

	@Test
	public void read() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(body.getBytes("UTF-8"));

		Unmarshaller unmarshaller = mock(Unmarshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();

		converter.setUnmarshaller(unmarshaller);

		when(unmarshaller.unmarshal(isA(StreamSource.class))).thenReturn(body);

		String result = (String) converter.read(Object.class, inputMessage);
		assertEquals("Invalid result", body, result);
	}

	@Test(expected = TypeMismatchException.class)
	public void readWithTypeMismatchException() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);

		Marshaller marshaller = mock(Marshaller.class);
		Unmarshaller unmarshaller = mock(Unmarshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller, unmarshaller);

		when(unmarshaller.unmarshal(isA(StreamSource.class))).thenReturn(Integer.valueOf(3));

		converter.read(String.class, inputMessage);
	}

	@Test
	public void readWithMarshallingFailureException() throws Exception {
		MockHttpInputMessage inputMessage = new MockHttpInputMessage(new byte[0]);
		UnmarshallingFailureException ex = new UnmarshallingFailureException("forced");

		Unmarshaller unmarshaller = mock(Unmarshaller.class);
		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter();

		converter.setUnmarshaller(unmarshaller);

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

		doNothing().when(marshaller).marshal(eq(body), isA(Result.class));

		converter.write(body, null, outputMessage);

		assertEquals("Invalid content-type", new MediaType("application", "xml"), outputMessage.getHeaders()
				.getContentType());
	}

	@Test
	public void writeWithMarshallingFailureException() throws Exception {
		String body = "<root>Hello World</root>";
		MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
		MarshallingFailureException ex = new MarshallingFailureException("forced");

		Marshaller marshaller = mock(Marshaller.class);

		MarshallingHttpMessageConverter converter = new MarshallingHttpMessageConverter(marshaller);

		doThrow(ex).when(marshaller).marshal(eq(body), isA(Result.class));

		try {
			converter.write(body, null, outputMessage);
			fail("HttpMessageNotWritableException should be thrown");
		}
		catch (HttpMessageNotWritableException e) {
			assertTrue("Invalid exception hierarchy", e.getCause() == ex);
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void supports() throws Exception {
		new MarshallingHttpMessageConverter().supports(Object.class);
	}
}
