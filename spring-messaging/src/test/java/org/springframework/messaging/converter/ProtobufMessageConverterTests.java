/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.messaging.converter;

import java.util.HashMap;
import java.util.Map;

import com.google.protobuf.ExtensionRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.protobuf.Msg;
import org.springframework.messaging.protobuf.SecondMsg;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.messaging.MessageHeaders.CONTENT_TYPE;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON;

/**
 * Test suite for {@link ProtobufMessageConverter}.
 *
 * @author Parviz Rozikov
 */
public class ProtobufMessageConverterTests {

	private ProtobufMessageConverter converter;

	private ExtensionRegistry extensionRegistry;

	private Msg testMsg;

	private Message<byte[]> message;

	private Message<byte[]> messageWithoutContentType;

	private Message<String> messageJson;


	@BeforeEach
	public void setup() {
		this.extensionRegistry = mock(ExtensionRegistry.class);
		this.converter = new ProtobufMessageConverter();
		this.testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();
		this.message = MessageBuilder.withPayload(this.testMsg.toByteArray())
				.setHeader(CONTENT_TYPE, ProtobufMessageConverter.PROTOBUF).build();
		this.messageWithoutContentType = MessageBuilder.withPayload(this.testMsg.toByteArray()).build();
		this.messageJson = MessageBuilder.withPayload(
					"{\n" +
					"  \"foo\": \"Foo\",\n" +
					"  \"blah\": {\n" +
					"    \"blah\": 123\n" +
					"  }\n" +
						"}")
				.setHeader(CONTENT_TYPE, APPLICATION_JSON)
				.build();
	}


	@Test
	public void extensionRegistryNull() {
		ProtobufMessageConverter converter = new ProtobufMessageConverter(null);
		assertThat(converter.extensionRegistry).isNotNull();
	}


	@Test
	public void canConvertFrom() {
		assertThat(converter.canConvertFrom(message, Msg.class)).isTrue();
		assertThat(converter.canConvertFrom(messageWithoutContentType, Msg.class)).isTrue();
		assertThat(converter.canConvertFrom(messageJson, Msg.class)).isTrue();
	}

	@Test
	public void canConvertTo() {
		assertThat(converter.canConvertTo(testMsg, message.getHeaders())).isTrue();
		assertThat(converter.canConvertTo(testMsg, messageWithoutContentType.getHeaders())).isTrue();
		assertThat(converter.canConvertTo(testMsg, messageJson.getHeaders())).isTrue();
	}


	@Test
	public void convertFrom() {
		final Msg msg = (Msg) converter.fromMessage(message, Msg.class);
		assertThat(msg).isEqualTo(testMsg);
	}

	@Test
	public void convertTo() {
		final Message<?> message = converter.toMessage(this.testMsg, this.message.getHeaders());
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(this.message.getPayload());
	}


	@Test
	public void convertFromNoContentType(){
		Msg result = (Msg) converter.fromMessage(messageWithoutContentType, Msg.class);
		assertThat(result).isEqualTo(testMsg);
	}


	@Test
	public void defaultContentType() {
		assertThat(converter.getDefaultContentType(testMsg)).isEqualTo(ProtobufMessageConverter.PROTOBUF);
	}

	@Test
	public void testJsonWithGoogleProtobuf() {
		this.converter = new ProtobufMessageConverter(
				new ProtobufMessageConverter.ProtobufJavaUtilSupport(null, null),
				extensionRegistry);

		final Map<String, Object> headers = new HashMap<>();
		headers.put(CONTENT_TYPE, APPLICATION_JSON);

		//convertTo
		final Message<?> message = this.converter.toMessage(this.testMsg, new MessageHeaders(headers));
		assertThat(message).isNotNull();
		assertThat(message.getHeaders().get(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
		assertThat(((String) message.getPayload()).length() > 0).isTrue();
		assertThat(((String) message.getPayload()).isEmpty()).as("Body is empty").isFalse();
		assertThat(((String) message.getPayload())).isEqualTo(this.messageJson.getPayload());

		//convertFrom
		final Msg msg = (Msg) converter.fromMessage(message, Msg.class);
		assertThat(msg).isEqualTo(this.testMsg);
	}

}
