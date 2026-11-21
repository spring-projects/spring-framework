/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.protobuf.ExtensionRegistry;
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

	private final Message<byte[]> messageJson = MessageBuilder.withPayload("""
			{
				"foo": "Foo",
				"blah": {
					"blah": 123
				}
			}
			""".getBytes(StandardCharsets.UTF_8))
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
		JSONAssert.assertEquals(new String(messageJson.getPayload()), message.getPayload().toString(), true);

		//convertFrom
		assertThat(converter.fromMessage(messageJson, Msg.class)).isEqualTo(testMsg);
	}

	@Test
	void jsonFormatConverterWithExtensionRegistry() {
		ExtensionRegistry registry = ExtensionRegistry.newInstance();
		assertThat(new ProtobufJsonFormatMessageConverter(registry).extensionRegistry).isSameAs(registry);
	}

}
