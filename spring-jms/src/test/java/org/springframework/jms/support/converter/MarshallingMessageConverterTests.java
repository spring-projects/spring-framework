/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jms.support.converter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

import jakarta.jms.BytesMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Arjen Poutsma
 */
class MarshallingMessageConverterTests {

	private Marshaller marshallerMock = mock();

	private Unmarshaller unmarshallerMock = mock();

	private Session sessionMock = mock();

	private MarshallingMessageConverter converter = new MarshallingMessageConverter(marshallerMock, unmarshallerMock);


	@Test
	void toBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock();
		Object toBeMarshalled = new Object();
		given(sessionMock.createBytesMessage()).willReturn(bytesMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(marshallerMock).marshal(eq(toBeMarshalled), isA(Result.class));
		verify(bytesMessageMock).writeBytes(isA(byte[].class));
	}

	@Test
	void fromBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock();
		Object unmarshalled = new Object();

		given(bytesMessageMock.getBodyLength()).willReturn(10L);
		given(bytesMessageMock.readBytes(isA(byte[].class))).willReturn(0);
		given(unmarshallerMock.unmarshal(isA(Source.class))).willReturn(unmarshalled);

		Object result = converter.fromMessage(bytesMessageMock);
		assertThat(unmarshalled).as("Invalid result").isEqualTo(result);
	}

	@Test
	void toTextMessage() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock();
		Object toBeMarshalled = new Object();

		given(sessionMock.createTextMessage(isA(String.class))).willReturn(textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(marshallerMock).marshal(eq(toBeMarshalled), isA(Result.class));
	}

	@Test
	void fromTextMessage() throws Exception {
		TextMessage textMessageMock = mock();
		Object unmarshalled = new Object();

		String text = "foo";
		given(textMessageMock.getText()).willReturn(text);
		given(unmarshallerMock.unmarshal(isA(Source.class))).willReturn(unmarshalled);

		Object result = converter.fromMessage(textMessageMock);
		assertThat(unmarshalled).as("Invalid result").isEqualTo(result);
	}

}
