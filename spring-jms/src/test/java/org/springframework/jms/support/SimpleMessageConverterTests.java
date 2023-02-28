/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jms.support;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;

import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.SimpleMessageConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for the {@link SimpleMessageConverter} class.
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 18.09.2004
 */
public class SimpleMessageConverterTests {

	@Test
	public void testStringConversion() throws JMSException {
		Session session = mock();
		TextMessage message = mock();

		String content = "test";

		given(session.createTextMessage(content)).willReturn(message);
		given(message.getText()).willReturn(content);

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertThat(converter.fromMessage(msg)).isEqualTo(content);
	}

	@Test
	public void testByteArrayConversion() throws JMSException {
		Session session = mock();
		BytesMessage message = mock();

		byte[] content = "test".getBytes();
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);

		given(session.createBytesMessage()).willReturn(message);
		given(message.getBodyLength()).willReturn((long) content.length);
		given(message.readBytes(any(byte[].class))).willAnswer(invocation -> byteArrayInputStream.read((byte[]) invocation.getArguments()[0]));

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertThat(((byte[]) converter.fromMessage(msg))).hasSize(content.length);

		verify(message).writeBytes(content);
	}

	@Test
	public void testMapConversion() throws JMSException {

		Session session = mock();
		MapMessage message = mock();

		Map<String, String> content = new HashMap<>(2);
		content.put("key1", "value1");
		content.put("key2", "value2");

		given(session.createMapMessage()).willReturn(message);
		given(message.getMapNames()).willReturn(Collections.enumeration(content.keySet()));
		given(message.getObject("key1")).willReturn("value1");
		given(message.getObject("key2")).willReturn("value2");

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertThat(converter.fromMessage(msg)).isEqualTo(content);

		verify(message).setObject("key1", "value1");
		verify(message).setObject("key2", "value2");
	}

	@Test
	public void testSerializableConversion() throws JMSException {
		Session session = mock();
		ObjectMessage message = mock();

		Integer content = 5;

		given(session.createObjectMessage(content)).willReturn(message);
		given(message.getObject()).willReturn(content);

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(content, session);
		assertThat(converter.fromMessage(msg)).isEqualTo(content);
	}

	@Test
	public void testToMessageThrowsExceptionIfGivenNullObjectToConvert() throws Exception {
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				new SimpleMessageConverter().toMessage(null, null));
	}

	@Test
	public void testToMessageThrowsExceptionIfGivenIncompatibleObjectToConvert() throws Exception {
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				new SimpleMessageConverter().toMessage(new Object(), null));
	}

	@Test
	public void testToMessageSimplyReturnsMessageAsIsIfSuppliedWithMessage() throws JMSException {
		Session session = mock();
		ObjectMessage message = mock();

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Message msg = converter.toMessage(message, session);
		assertThat(msg).isSameAs(message);
	}

	@Test
	public void testFromMessageSimplyReturnsMessageAsIsIfSuppliedWithMessage() throws JMSException {
		Message message = mock();

		SimpleMessageConverter converter = new SimpleMessageConverter();
		Object msg = converter.fromMessage(message);
		assertThat(msg).isSameAs(message);
	}

	@Test
	public void testMapConversionWhereMapHasNonStringTypesForKeys() throws JMSException {
		MapMessage message = mock();
		Session session = mock();
		given(session.createMapMessage()).willReturn(message);

		Map<Integer, String> content = new HashMap<>(1);
		content.put(1, "value1");

		SimpleMessageConverter converter = new SimpleMessageConverter();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				converter.toMessage(content, session));
	}

	@Test
	public void testMapConversionWhereMapHasNNullForKey() throws JMSException {
		MapMessage message = mock();
		Session session = mock();
		given(session.createMapMessage()).willReturn(message);

		Map<Object, String> content = new HashMap<>(1);
		content.put(null, "value1");

		SimpleMessageConverter converter = new SimpleMessageConverter();
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				converter.toMessage(content, session));
	}

}
