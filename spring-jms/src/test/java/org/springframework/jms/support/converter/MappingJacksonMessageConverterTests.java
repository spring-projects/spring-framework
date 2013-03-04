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

package org.springframework.jms.support.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * @author Arjen Poutsma
 * @author Dave Syer
 */
public class MappingJacksonMessageConverterTests {

	private MappingJacksonMessageConverter converter;

	private Session sessionMock;

	@Before
	public void setUp() throws Exception {
		sessionMock = mock(Session.class);
		converter = new MappingJacksonMessageConverter();
		converter.setEncodingPropertyName("__encoding__");
		converter.setTypeIdPropertyName("__typeid__");
	}

	@Test
	public void toBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock(BytesMessage.class);
		Object toBeMarshalled = new Object();

		given(sessionMock.createBytesMessage()).willReturn(bytesMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(bytesMessageMock).setStringProperty("__encoding__", "UTF-8");
		verify(bytesMessageMock).setStringProperty("__typeid__", Object.class.getName());
		verify(bytesMessageMock).writeBytes(isA(byte[].class));
	}

	@Test
	@SuppressWarnings("serial")
	public void fromBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock(BytesMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		byte[] bytes = "{\"foo\":\"bar\"}".getBytes();
		final ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		given(bytesMessageMock.getStringProperty("__typeid__")).willReturn(
				Object.class.getName());
		given(bytesMessageMock.propertyExists("__encoding__")).willReturn(false);
		given(bytesMessageMock.getBodyLength()).willReturn(new Long(bytes.length));
		given(bytesMessageMock.readBytes(any(byte[].class))).willAnswer(
				new Answer<Integer>() {

					@Override
					public Integer answer(InvocationOnMock invocation) throws Throwable {
						return byteStream.read((byte[]) invocation.getArguments()[0]);
					}
				});

		Object result = converter.fromMessage(bytesMessageMock);
		assertEquals("Invalid result", result, unmarshalled);
	}

	@Test
	public void toTextMessageWithObject() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);
		Object toBeMarshalled = new Object();

		given(sessionMock.createTextMessage(isA(String.class))).willReturn(
				textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);
		verify(textMessageMock).setStringProperty("__typeid__", Object.class.getName());
	}

	@Test
	public void toTextMessageWithMap() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);
		Map<String, String> toBeMarshalled = new HashMap<String, String>();
		toBeMarshalled.put("foo", "bar");
		given(sessionMock.createTextMessage(isA(String.class))).willReturn(
				textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(textMessageMock).setStringProperty("__typeid__", HashMap.class.getName());
	}

	@Test
	public void fromTextMessageAsObject() throws Exception {
		TextMessage textMessageMock = mock(TextMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		String text = "{\"foo\":\"bar\"}";
		given(textMessageMock.getStringProperty("__typeid__")).willReturn(
				Object.class.getName());
		given(textMessageMock.getText()).willReturn(text);

		Object result = converter.fromMessage(textMessageMock);
		assertEquals("Invalid result", result, unmarshalled);
	}

	@Test
	public void fromTextMessageAsMap() throws Exception {
		TextMessage textMessageMock = mock(TextMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		String text = "{\"foo\":\"bar\"}";
		given(textMessageMock.getStringProperty("__typeid__")).willReturn(
				HashMap.class.getName());
		given(textMessageMock.getText()).willReturn(text);

		Object result = converter.fromMessage(textMessageMock);
		assertEquals("Invalid result", result, unmarshalled);
	}
}
