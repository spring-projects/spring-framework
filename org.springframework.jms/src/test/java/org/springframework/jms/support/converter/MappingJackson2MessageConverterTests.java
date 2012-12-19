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

package org.springframework.jms.support.converter;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.jms.BytesMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * @author Arjen Poutsma
 * @author Dave Syer
 */
public class MappingJackson2MessageConverterTests {

	private MappingJackson2MessageConverter converter;

	private Session sessionMock;

	@Before
	public void setUp() throws Exception {
		sessionMock = createMock(Session.class);
		converter = new MappingJackson2MessageConverter();
		converter.setEncodingPropertyName("__encoding__");
		converter.setTypeIdPropertyName("__typeid__");
	}

	@Test
	public void toBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = createMock(BytesMessage.class);
		Date toBeMarshalled = new Date();

		expect(sessionMock.createBytesMessage()).andReturn(bytesMessageMock);
		bytesMessageMock.setStringProperty("__encoding__", "UTF-8");
		bytesMessageMock.setStringProperty("__typeid__", Date.class.getName());
		bytesMessageMock.writeBytes(isA(byte[].class));

		replay(sessionMock, bytesMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(sessionMock, bytesMessageMock);
	}

	@Test
	public void fromBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = createMock(BytesMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		final byte[] bytes = "{\"foo\":\"bar\"}".getBytes();
		Capture<byte[]> captured = new Capture<byte[]>() {
			@Override
			public void setValue(byte[] value) {
				super.setValue(value);
				System.arraycopy(bytes, 0, value, 0, bytes.length);
			}
		};

		expect(
				bytesMessageMock.getStringProperty("__typeid__"))
				.andReturn(Object.class.getName());
		expect(
				bytesMessageMock.propertyExists("__encoding__"))
				.andReturn(false);
		expect(bytesMessageMock.getBodyLength()).andReturn(
				new Long(bytes.length));
		expect(bytesMessageMock.readBytes(EasyMock.capture(captured)))
				.andReturn(bytes.length);

		replay(sessionMock, bytesMessageMock);

		Object result = converter.fromMessage(bytesMessageMock);
		assertEquals("Invalid result", result, unmarshalled);

		verify(sessionMock, bytesMessageMock);
	}

	@Test
	public void toTextMessageWithObject() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = createMock(TextMessage.class);
		Date toBeMarshalled = new Date();

		textMessageMock.setStringProperty("__typeid__", Date.class.getName());
		expect(sessionMock.createTextMessage(isA(String.class))).andReturn( textMessageMock);

		replay(sessionMock, textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(sessionMock, textMessageMock);
	}

	@Test
	public void toTextMessageWithMap() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = createMock(TextMessage.class);
		Map<String, String> toBeMarshalled = new HashMap<String, String>();
		toBeMarshalled.put("foo", "bar");

		textMessageMock.setStringProperty("__typeid__", HashMap.class.getName());
		expect(sessionMock.createTextMessage(isA(String.class))).andReturn(
				textMessageMock);

		replay(sessionMock, textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(sessionMock, textMessageMock);
	}

	@Test
	public void fromTextMessageAsObject() throws Exception {
		TextMessage textMessageMock = createMock(TextMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		String text = "{\"foo\":\"bar\"}";
		expect(
				textMessageMock.getStringProperty("__typeid__"))
				.andReturn(Object.class.getName());
		expect(textMessageMock.getText()).andReturn(text);

		replay(sessionMock, textMessageMock);

		Object result = converter.fromMessage(textMessageMock);
		assertEquals("Invalid result", result, unmarshalled);

		verify(sessionMock, textMessageMock);
	}

	@Test
	public void fromTextMessageAsMap() throws Exception {
		TextMessage textMessageMock = createMock(TextMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		String text = "{\"foo\":\"bar\"}";
		expect(
				textMessageMock.getStringProperty("__typeid__"))
				.andReturn(HashMap.class.getName());
		expect(textMessageMock.getText()).andReturn(text);

		replay(sessionMock, textMessageMock);

		Object result = converter.fromMessage(textMessageMock);
		assertEquals("Invalid result", result, unmarshalled);

		verify(sessionMock, textMessageMock);
	}

}
