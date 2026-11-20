/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
