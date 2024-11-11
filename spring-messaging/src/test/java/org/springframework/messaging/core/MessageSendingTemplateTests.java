/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.messaging.core;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AbstractMessageSendingTemplate}.
 *
 * @author Rossen Stoyanchev
 */
class MessageSendingTemplateTests {

	private TestMessageSendingTemplate template;

	private TestMessagePostProcessor postProcessor;

	private Map<String, Object> headers;


	@BeforeEach
	void setup() {
		this.template = new TestMessageSendingTemplate();
		this.postProcessor = new TestMessagePostProcessor();
		this.headers = new HashMap<>();
		this.headers.put("key", "value");
	}

	@Test
	void send() {
		Message<?> message = new GenericMessage<Object>("payload");
		this.template.setDefaultDestination("home");
		this.template.send(message);

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(this.template.message).isSameAs(message);
	}

	@Test
	void sendToDestination() {
		Message<?> message = new GenericMessage<Object>("payload");
		this.template.send("somewhere", message);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.message).isSameAs(message);
	}

	@Test
	void sendMissingDestination() {
		Message<?> message = new GenericMessage<Object>("payload");
		assertThatIllegalStateException().isThrownBy(() ->
				this.template.send(message));
	}

	@Test
	void convertAndSend() {
		this.template.convertAndSend("somewhere", "payload", headers, this.postProcessor);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertThat(this.postProcessor.getMessage()).isNotNull();
		assertThat(this.postProcessor.getMessage()).isSameAs(this.template.message);
	}

	@Test
	void convertAndSendPayload() {
		this.template.setDefaultDestination("home");
		this.template.convertAndSend("payload");

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders()).as("expected 'id' and 'timestamp' headers only").hasSize(2);
		assertThat(this.template.message.getPayload()).isEqualTo("payload");
	}

	@Test
	void convertAndSendPayloadToDestination() {
		this.template.convertAndSend("somewhere", "payload");

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders()).as("expected 'id' and 'timestamp' headers only").hasSize(2);
		assertThat(this.template.message.getPayload()).isEqualTo("payload");
	}

	@Test
	void convertAndSendPayloadAndHeadersToDestination() {
		this.template.convertAndSend("somewhere", "payload", headers);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("payload");
	}

	@Test
	void convertAndSendPayloadAndMutableHeadersToDestination() {
		MessageHeaderAccessor accessor = new MessageHeaderAccessor();
		accessor.setHeader("foo", "bar");
		accessor.setLeaveMutable(true);
		MessageHeaders messageHeaders = accessor.getMessageHeaders();

		this.template.setMessageConverter(new StringMessageConverter());
		this.template.convertAndSend("somewhere", "payload", messageHeaders);

		MessageHeaders actual = this.template.message.getHeaders();
		assertThat(actual).isSameAs(messageHeaders);
		assertThat(actual.get(MessageHeaders.CONTENT_TYPE)).isEqualTo(new MimeType("text", "plain", StandardCharsets.UTF_8));
		assertThat(actual.get("foo")).isEqualTo("bar");
	}

	@Test
	void convertAndSendPayloadWithPostProcessor() {
		this.template.setDefaultDestination("home");
		this.template.convertAndSend((Object) "payload", this.postProcessor);

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders()).as("expected 'id' and 'timestamp' headers only").hasSize(2);
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertThat(this.postProcessor.getMessage()).isNotNull();
		assertThat(this.postProcessor.getMessage()).isSameAs(this.template.message);
	}

	@Test
	void convertAndSendPayloadWithPostProcessorToDestination() {
		this.template.convertAndSend("somewhere", "payload", this.postProcessor);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders()).as("expected 'id' and 'timestamp' headers only").hasSize(2);
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertThat(this.postProcessor.getMessage()).isNotNull();
		assertThat(this.postProcessor.getMessage()).isSameAs(this.template.message);
	}

	@Test
	void convertAndSendNoMatchingConverter() {

		MessageConverter converter = new CompositeMessageConverter(
				List.of(new MappingJackson2MessageConverter()));
		this.template.setMessageConverter(converter);

		this.headers.put(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_XML);
		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.template.convertAndSend("home", "payload", new MessageHeaders(this.headers)));
	}


	private static class TestMessageSendingTemplate extends AbstractMessageSendingTemplate<String> {

		private String destination;

		private Message<?> message;

		@Override
		protected void doSend(String destination, Message<?> message) {
			this.destination = destination;
			this.message = message;
		}
	}

}

class TestMessagePostProcessor implements MessagePostProcessor {

	private Message<?> message;


	Message<?> getMessage() {
		return this.message;
	}

	@Override
	public Message<?> postProcessMessage(Message<?> message) {
		this.message = message;
		return message;
	}
}
