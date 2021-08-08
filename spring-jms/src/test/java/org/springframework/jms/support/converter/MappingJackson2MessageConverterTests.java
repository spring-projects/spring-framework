/*
 * Copyright 2002-2021 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import org.springframework.core.MethodParameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Arjen Poutsma
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class MappingJackson2MessageConverterTests {

	private MappingJackson2MessageConverter converter;

	private Session sessionMock;


	@BeforeEach
	public void setup() {
		sessionMock = mock(Session.class);
		converter = new MappingJackson2MessageConverter();
		converter.setEncodingPropertyName("__encoding__");
		converter.setTypeIdPropertyName("__typeid__");
	}


	@Test
	void toBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock(BytesMessage.class);
		Date toBeMarshalled = new Date();

		given(sessionMock.createBytesMessage()).willReturn(bytesMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);

		verify(bytesMessageMock).setStringProperty("__encoding__", "UTF-8");
		verify(bytesMessageMock).setStringProperty("__typeid__", Date.class.getName());
		verify(bytesMessageMock).writeBytes(isA(byte[].class));
	}

	@Test
	void fromBytesMessage() throws Exception {
		BytesMessage bytesMessageMock = mock(BytesMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		byte[] bytes = "{\"foo\":\"bar\"}".getBytes();
		final ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);

		given(bytesMessageMock.getStringProperty("__typeid__")).willReturn(Object.class.getName());
		given(bytesMessageMock.propertyExists("__encoding__")).willReturn(false);
		given(bytesMessageMock.getBodyLength()).willReturn(Long.valueOf(bytes.length));
		given(bytesMessageMock.readBytes(any(byte[].class))).willAnswer(
				(Answer<Integer>) invocation -> byteStream.read((byte[]) invocation.getArguments()[0]));

		Object result = converter.fromMessage(bytesMessageMock);
		assertThat(unmarshalled).as("Invalid result").isEqualTo(result);
	}

	@Test
	void toTextMessageWithObject() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);
		Date toBeMarshalled = new Date();

		given(sessionMock.createTextMessage(isA(String.class))).willReturn(textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);
		verify(textMessageMock).setStringProperty("__typeid__", Date.class.getName());
	}

	@Test
	void toTextMessageWithMap() throws Exception {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);
		Map<String, String> toBeMarshalled = new HashMap<>();
		toBeMarshalled.put("foo", "bar");

		given(sessionMock.createTextMessage(isA(String.class))).willReturn(textMessageMock);

		converter.toMessage(toBeMarshalled, sessionMock);
		verify(textMessageMock).setStringProperty("__typeid__", HashMap.class.getName());
	}

	@Test
	void fromTextMessage() throws Exception {
		TextMessage textMessageMock = mock(TextMessage.class);
		MyBean unmarshalled = new MyBean("bar");

		String text = "{\"foo\":\"bar\"}";
		given(textMessageMock.getStringProperty("__typeid__")).willReturn(MyBean.class.getName());
		given(textMessageMock.getText()).willReturn(text);

		MyBean result = (MyBean)converter.fromMessage(textMessageMock);
		assertThat(unmarshalled).as("Invalid result").isEqualTo(result);
	}

	@Test
	void fromTextMessageWithUnknownProperty() throws Exception {
		TextMessage textMessageMock = mock(TextMessage.class);
		MyBean unmarshalled = new MyBean("bar");

		String text = "{\"foo\":\"bar\", \"unknownProperty\":\"value\"}";
		given(textMessageMock.getStringProperty("__typeid__")).willReturn(MyBean.class.getName());
		given(textMessageMock.getText()).willReturn(text);

		MyBean result = (MyBean)converter.fromMessage(textMessageMock);
		assertThat(unmarshalled).as("Invalid result").isEqualTo(result);
	}

	@Test
	void fromTextMessageAsObject() throws Exception {
		TextMessage textMessageMock = mock(TextMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		String text = "{\"foo\":\"bar\"}";
		given(textMessageMock.getStringProperty("__typeid__")).willReturn(Object.class.getName());
		given(textMessageMock.getText()).willReturn(text);

		Object result = converter.fromMessage(textMessageMock);
		assertThat(unmarshalled).as("Invalid result").isEqualTo(result);
	}

	@Test
	void fromTextMessageAsMap() throws Exception {
		TextMessage textMessageMock = mock(TextMessage.class);
		Map<String, String> unmarshalled = Collections.singletonMap("foo", "bar");

		String text = "{\"foo\":\"bar\"}";
		given(textMessageMock.getStringProperty("__typeid__")).willReturn(HashMap.class.getName());
		given(textMessageMock.getText()).willReturn(text);

		Object result = converter.fromMessage(textMessageMock);
		assertThat(unmarshalled).as("Invalid result").isEqualTo(result);
	}

	@Test
	void toTextMessageWithReturnType() throws JMSException, NoSuchMethodException {
		Method method = this.getClass().getDeclaredMethod("summary");
		MethodParameter returnType = new MethodParameter(method, -1);
		testToTextMessageWithReturnType(returnType);
		verify(sessionMock).createTextMessage("{\"name\":\"test\"}");
	}

	@Test
	void toTextMessageWithNullReturnType() throws JMSException, NoSuchMethodException {
		testToTextMessageWithReturnType(null);
		verify(sessionMock).createTextMessage("{\"name\":\"test\",\"description\":\"lengthy description\"}");
	}

	@Test
	void toTextMessageWithReturnTypeAndNoJsonView() throws JMSException, NoSuchMethodException {
		Method method = this.getClass().getDeclaredMethod("none");
		MethodParameter returnType = new MethodParameter(method, -1);

		testToTextMessageWithReturnType(returnType);
		verify(sessionMock).createTextMessage("{\"name\":\"test\",\"description\":\"lengthy description\"}");
	}

	@Test
	void toTextMessageWithReturnTypeAndMultipleJsonViews() throws JMSException, NoSuchMethodException {
		Method method = this.getClass().getDeclaredMethod("invalid");
		MethodParameter returnType = new MethodParameter(method, -1);

		assertThatIllegalArgumentException().isThrownBy(() ->
				testToTextMessageWithReturnType(returnType));
	}

	private void testToTextMessageWithReturnType(MethodParameter returnType) throws JMSException, NoSuchMethodException {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);

		MyAnotherBean bean = new MyAnotherBean("test", "lengthy description");
		given(sessionMock.createTextMessage(isA(String.class))).willReturn(textMessageMock);
		converter.toMessage(bean, sessionMock, returnType);
		verify(textMessageMock).setStringProperty("__typeid__", MyAnotherBean.class.getName());
	}

	@Test
	void toTextMessageWithJsonViewClass() throws JMSException {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);

		MyAnotherBean bean = new MyAnotherBean("test", "lengthy description");
		given(sessionMock.createTextMessage(isA(String.class))).willReturn(textMessageMock);


		converter.toMessage(bean, sessionMock, Summary.class);
		verify(textMessageMock).setStringProperty("__typeid__", MyAnotherBean.class.getName());
		verify(sessionMock).createTextMessage("{\"name\":\"test\"}");
	}

	@Test
	void toTextMessageWithAnotherJsonViewClass() throws JMSException {
		converter.setTargetType(MessageType.TEXT);
		TextMessage textMessageMock = mock(TextMessage.class);

		MyAnotherBean bean = new MyAnotherBean("test", "lengthy description");
		given(sessionMock.createTextMessage(isA(String.class))).willReturn(textMessageMock);


		converter.toMessage(bean, sessionMock, Full.class);
		verify(textMessageMock).setStringProperty("__typeid__", MyAnotherBean.class.getName());
		verify(sessionMock).createTextMessage("{\"name\":\"test\",\"description\":\"lengthy description\"}");
	}


	@JsonView(Summary.class)
	public MyAnotherBean summary() {
		return new MyAnotherBean();
	}

	public MyAnotherBean none() {
		return new MyAnotherBean();
	}

	@JsonView({Summary.class, Full.class})
	public MyAnotherBean invalid() {
		return new MyAnotherBean();
	}


	public static class MyBean {

		private String foo;

		public MyBean() {
		}

		public MyBean(String foo) {
			this.foo = foo;
		}

		public String getFoo() {
			return foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			MyBean bean = (MyBean) o;
			if (foo != null ? !foo.equals(bean.foo) : bean.foo != null) {
				return false;
			}
			return true;
		}

		@Override
		public int hashCode() {
			return foo != null ? foo.hashCode() : 0;
		}
	}


	private interface Summary {};

	private interface Full extends Summary {};


	@SuppressWarnings("unused")
	private static class MyAnotherBean {

		@JsonView(Summary.class)
		private String name;

		@JsonView(Full.class)
		private String description;

		private MyAnotherBean() {
		}

		public MyAnotherBean(String name, String description) {
			this.name = name;
			this.description = description;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(String description) {
			this.description = description;
		}
	}

}
