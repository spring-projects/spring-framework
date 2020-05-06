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

package org.springframework.messaging.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link AbstractDestinationResolvingMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 */
public class DestinationResolvingMessagingTemplateTests {

	private TestDestinationResolvingMessagingTemplate template;

	private ExecutorSubscribableChannel myChannel;

	private Map<String, Object> headers;

	private TestMessagePostProcessor postProcessor;


	@BeforeEach
	public void setup() {

		TestMessageChannelDestinationResolver resolver = new TestMessageChannelDestinationResolver();

		this.myChannel = new ExecutorSubscribableChannel();
		resolver.registerMessageChannel("myChannel", this.myChannel);

		this.template = new TestDestinationResolvingMessagingTemplate();
		this.template.setDestinationResolver(resolver);

		this.headers = Collections.<String, Object>singletonMap("key", "value");

		this.postProcessor = new TestMessagePostProcessor();
	}


	@Test
	public void send() {
		Message<?> message = new GenericMessage<Object>("payload");
		this.template.send("myChannel", message);

		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
		assertThat(this.template.message).isSameAs(message);
	}

	@Test
	public void sendNoDestinationResolver() {
		TestDestinationResolvingMessagingTemplate template = new TestDestinationResolvingMessagingTemplate();
		assertThatIllegalStateException().isThrownBy(() ->
				template.send("myChannel", new GenericMessage<Object>("payload")));
	}

	@Test
	public void convertAndSendPayload() {
		this.template.convertAndSend("myChannel", "payload");

		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getPayload()).isSameAs("payload");
	}

	@Test
	public void convertAndSendPayloadAndHeaders() {
		this.template.convertAndSend("myChannel", "payload", this.headers);

		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("payload");
	}

	@Test
	public void convertAndSendPayloadWithPostProcessor() {
		this.template.convertAndSend("myChannel", "payload", this.postProcessor);

		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertThat(this.postProcessor.getMessage()).isNotNull();
		assertThat(this.template.message).isSameAs(this.postProcessor.getMessage());
	}

	@Test
	public void convertAndSendPayloadAndHeadersWithPostProcessor() {
		this.template.convertAndSend("myChannel", "payload", this.headers, this.postProcessor);

		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
		assertThat(this.template.message).isNotNull();
		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("payload");

		assertThat(this.postProcessor.getMessage()).isNotNull();
		assertThat(this.template.message).isSameAs(this.postProcessor.getMessage());
	}

	@Test
	public void receive() {
		Message<?> expected = new GenericMessage<Object>("payload");
		this.template.setReceiveMessage(expected);
		Message<?> actual = this.template.receive("myChannel");

		assertThat(actual).isSameAs(expected);
		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
	}

	@Test
	public void receiveAndConvert() {
		Message<?> expected = new GenericMessage<Object>("payload");
		this.template.setReceiveMessage(expected);
		String payload = this.template.receiveAndConvert("myChannel", String.class);

		assertThat(payload).isEqualTo("payload");
		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
	}

	@Test
	public void sendAndReceive() {
		Message<?> requestMessage = new GenericMessage<Object>("request");
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		Message<?> actual = this.template.sendAndReceive("myChannel", requestMessage);

		assertThat(this.template.message).isEqualTo(requestMessage);
		assertThat(actual).isSameAs(responseMessage);
		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
	}

	@Test
	public void convertSendAndReceive() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String actual = this.template.convertSendAndReceive("myChannel", "request", String.class);

		assertThat(this.template.message.getPayload()).isEqualTo("request");
		assertThat(actual).isSameAs("response");
		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
	}

	@Test
	public void convertSendAndReceiveWithHeaders() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String actual = this.template.convertSendAndReceive("myChannel", "request", this.headers, String.class);

		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("request");
		assertThat(actual).isSameAs("response");
		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
	}

	@Test
	public void convertSendAndReceiveWithPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String actual = this.template.convertSendAndReceive("myChannel", "request", String.class, this.postProcessor);

		assertThat(this.template.message.getPayload()).isEqualTo("request");
		assertThat(this.postProcessor.getMessage().getPayload()).isSameAs("request");
		assertThat(actual).isSameAs("response");
		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
	}

	@Test
	public void convertSendAndReceiveWithHeadersAndPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String actual = this.template.convertSendAndReceive("myChannel", "request", this.headers,
				String.class, this.postProcessor);

		assertThat(this.template.message.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.message.getPayload()).isEqualTo("request");
		assertThat(this.postProcessor.getMessage().getPayload()).isSameAs("request");
		assertThat(actual).isSameAs("response");
		assertThat(this.template.messageChannel).isSameAs(this.myChannel);
	}


	private static class TestDestinationResolvingMessagingTemplate
			extends AbstractDestinationResolvingMessagingTemplate<MessageChannel> {

		private MessageChannel messageChannel;

		private Message<?> message;

		private Message<?> receiveMessage;


		private void setReceiveMessage(Message<?> receiveMessage) {
			this.receiveMessage = receiveMessage;
		}

		@Override
		protected void doSend(MessageChannel channel, Message<?> message) {
			this.messageChannel = channel;
			this.message = message;
		}

		@Override
		protected Message<?> doReceive(MessageChannel channel) {
			this.messageChannel = channel;
			return this.receiveMessage;
		}

		@Override
		protected Message<?> doSendAndReceive(MessageChannel channel, Message<?> requestMessage) {
			this.message = requestMessage;
			this.messageChannel = channel;
			return this.receiveMessage;
		}
	}

}

class TestMessageChannelDestinationResolver implements DestinationResolver<MessageChannel> {

	private final Map<String, MessageChannel> channels = new HashMap<>();


	public void registerMessageChannel(String name, MessageChannel channel) {
		this.channels.put(name, channel);
	}

	@Override
	public MessageChannel resolveDestination(String name) throws DestinationResolutionException {
		return this.channels.get(name);
	}
}
