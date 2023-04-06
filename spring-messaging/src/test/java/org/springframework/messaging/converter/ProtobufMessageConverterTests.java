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

package org.springframework.messaging.converter;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

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
 * Tests for {@link ProtobufMessageConverter}.
 *
 * @author Parviz Rozikov
 * @author Sam Brannen
 */
class ProtobufMessageConverterTests {

	private final ProtobufMessageConverter converter = new ProtobufMessageConverter();

	private Msg testMsg = Msg.newBuilder().setFoo("Foo").setBlah(SecondMsg.newBuilder().setBlah(123).build()).build();

	private Message<byte[]> message = MessageBuilder.withPayload(this.testMsg.toByteArray())
			.setHeader(CONTENT_TYPE, ProtobufMessageConverter.PROTOBUF).build();

	private Message<byte[]> messageWithoutContentType = MessageBuilder.withPayload(this.testMsg.toByteArray()).build();

	private final Message<String> messageJson = MessageBuilder.withPayload("""
			{
				"foo": "Foo",
				"blah": {
					"blah": 123
				}
			}
			""")
			.setHeader(CONTENT_TYPE, APPLICATION_JSON)
			.build();


	@Test
	void extensionRegistryNull() {
		ProtobufMessageConverter converter = new ProtobufMessageConverter(null);
		assertThat(converter.extensionRegistry).isNotNull();
	}

	@Test
	void defaultContentType() {
		assertThat(converter.getDefaultContentType(testMsg)).isEqualTo(ProtobufMessageConverter.PROTOBUF);
	}

	@Test
	void canConvertFrom() {
		assertThat(converter.canConvertFrom(message, Msg.class)).isTrue();
		assertThat(converter.canConvertFrom(messageWithoutContentType, Msg.class)).isTrue();
		assertThat(converter.canConvertFrom(messageJson, Msg.class)).isTrue();
	}

	@Test
	void canConvertTo() {
		assertThat(converter.canConvertTo(testMsg, message.getHeaders())).isTrue();
		assertThat(converter.canConvertTo(testMsg, messageWithoutContentType.getHeaders())).isTrue();
		assertThat(converter.canConvertTo(testMsg, messageJson.getHeaders())).isTrue();
	}

	@Test
	void convertFrom() {
		assertThat(converter.fromMessage(message, Msg.class)).isEqualTo(testMsg);
	}

	@Test
	void convertFromNoContentType(){
		assertThat(converter.fromMessage(messageWithoutContentType, Msg.class)).isEqualTo(testMsg);
	}

	@Test
	void convertTo() {
		Message<?> message = converter.toMessage(testMsg, this.message.getHeaders());
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo(this.message.getPayload());
	}

	@Test
	void jsonWithGoogleProtobuf() throws Exception {
		ProtobufMessageConverter converter = new ProtobufMessageConverter(
				new ProtobufMessageConverter.ProtobufJavaUtilSupport(null, null),
				mock());

		//convertTo
		Message<?> message = converter.toMessage(testMsg, new MessageHeaders(Map.of(CONTENT_TYPE, APPLICATION_JSON)));
		assertThat(message).isNotNull();
		assertThat(message.getHeaders().get(CONTENT_TYPE)).isEqualTo(APPLICATION_JSON);
		JSONAssert.assertEquals(messageJson.getPayload(), message.getPayload().toString(), true);

		//convertFrom
		assertThat(converter.fromMessage(message, Msg.class)).isEqualTo(testMsg);
	}

}
