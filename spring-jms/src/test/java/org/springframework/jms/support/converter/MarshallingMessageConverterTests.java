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

import javax.jms.BytesMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Result;
import javax.xml.transform.Source;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

/**
 * @author Arjen Poutsma
 */
public class MarshallingMessageConverterTests {

	private MarshallingMessageConverter converter;

	private Marshaller marshallerMock;

	private Unmarshaller unmarshallerMock;

	private Session sessionMock;

	@Before
	public void setUp() throws Exception {
		marshallerMock = createMock(Marshaller.class);
		unmarshallerMock = createMock(Unmarshaller.class);
		sessionMock = createMock(Session.class);
		converter = new MarshallingMessageConverter(marshallerMock, unmarshallerMock);
	}

	@Test
	public void toBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = createMock(BytesMessage.class);
		Object toBeMarshalled = new Object();

		expect(sessionMock.createBytesMessage()).andReturn(bytesMessageMock);
		marshallerMock.marshal(eq(toBeMarshalled), isA(Result.class));
		bytesMessageMock.writeBytes(isA(byte[].class));

		replay(marshallerMock, unmarshallerMock, sessionMock, bytesMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(marshallerMock, unmarshallerMock, sessionMock, bytesMessageMock);
	}

	@Test
	public void fromBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = createMock(BytesMessage.class);
		Object unmarshalled = new Object();

		expect(bytesMessageMock.getBodyLength()).andReturn(10L);
		expect(bytesMessageMock.readBytes(isA(byte[].class))).andReturn(0);
		expect(unmarshallerMock.unmarshal(isA(Source.class))).andReturn(unmarshalled);

		replay(marshallerMock, unmarshallerMock, sessionMock, bytesMessageMock);

		Object result = converter.fromMessage(bytesMessageMock);
		assertEquals("Invalid result", result, unmarshalled);

		verify(marshallerMock, unmarshallerMock, sessionMock, bytesMessageMock);
	}

	@Test
	public void toTextMessage() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = createMock(TextMessage.class);
		Object toBeMarshalled = new Object();

		expect(sessionMock.createTextMessage(isA(String.class))).andReturn(textMessageMock);
		marshallerMock.marshal(eq(toBeMarshalled), isA(Result.class));

		replay(marshallerMock, unmarshallerMock, sessionMock, textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(marshallerMock, unmarshallerMock, sessionMock, textMessageMock);
	}

	@Test
	public void fromTextMessage() throws Exception {
		TextMessage textMessageMock = createMock(TextMessage.class);
		Object unmarshalled = new Object();

		String text = "foo";
		expect(textMessageMock.getText()).andReturn(text);
		expect(unmarshallerMock.unmarshal(isA(Source.class))).andReturn(unmarshalled);

		replay(marshallerMock, unmarshallerMock, sessionMock, textMessageMock);

		Object result = converter.fromMessage(textMessageMock);
		assertEquals("Invalid result", result, unmarshalled);

		verify(marshallerMock, unmarshallerMock, sessionMock, textMessageMock);
	}

}
