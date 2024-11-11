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

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for request and reply operations in {@link AbstractMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 * @see MessageReceivingTemplateTests
 */
class MessageRequestReplyTemplateTests {

	private TestMessagingTemplate template;

	private TestMessagePostProcessor postProcessor;

	private Map<String, Object> headers;


	@BeforeEach
	void setup() {
		this.template = new TestMessagingTemplate();
		this.postProcessor = new TestMessagePostProcessor();
		this.headers = Collections.singletonMap("key", "value");
	}


	@Test
	void sendAndReceive() {
		Message<?> requestMessage = new GenericMessage<Object>("request");
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(responseMessage);
		Message<?> actual = this.template.sendAndReceive(requestMessage);

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(this.template.requestMessage).isSameAs(requestMessage);
		assertThat(actual).isSameAs(responseMessage);
	}

	@Test
	void sendAndReceiveMissingDestination() {
		assertThatIllegalStateException().isThrownBy(() ->
				this.template.sendAndReceive(new GenericMessage<>("request")));
	}

	@Test
	void sendAndReceiveToDestination() {
		Message<?> requestMessage = new GenericMessage<Object>("request");
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		Message<?> actual = this.template.sendAndReceive("somewhere", requestMessage);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.requestMessage).isSameAs(requestMessage);
		assertThat(actual).isSameAs(responseMessage);
	}

	@Test
	void convertAndSend() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("request", String.class);

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(this.template.requestMessage.getPayload()).isSameAs("request");
		assertThat(response).isSameAs("response");
	}

	@Test
	void convertAndSendToDestination() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", String.class);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.requestMessage.getPayload()).isSameAs("request");
		assertThat(response).isSameAs("response");
	}

	@Test
	void convertAndSendToDestinationWithHeaders() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", this.headers, String.class);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.requestMessage.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.requestMessage.getPayload()).isSameAs("request");
		assertThat(response).isSameAs("response");
	}

	@Test
	void convertAndSendWithPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("request", String.class, this.postProcessor);

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(this.template.requestMessage.getPayload()).isSameAs("request");
		assertThat(response).isSameAs("response");
		assertThat(this.template.requestMessage).isSameAs(this.postProcessor.getMessage());
	}

	@Test
	void convertAndSendToDestinationWithPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", String.class, this.postProcessor);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.requestMessage.getPayload()).isSameAs("request");
		assertThat(response).isSameAs("response");
		assertThat(this.template.requestMessage).isSameAs(this.postProcessor.getMessage());
	}

	@Test
	void convertAndSendToDestinationWithHeadersAndPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", this.headers,
				String.class, this.postProcessor);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(this.template.requestMessage.getHeaders().get("key")).isEqualTo("value");
		assertThat(this.template.requestMessage.getPayload()).isSameAs("request");
		assertThat(response).isSameAs("response");
		assertThat(this.template.requestMessage).isSameAs(this.postProcessor.getMessage());
	}


	private static class TestMessagingTemplate extends AbstractMessagingTemplate<String> {

		private String destination;

		private Message<?> requestMessage;

		private Message<?> receiveMessage;


		private void setReceiveMessage(Message<?> receiveMessage) {
			this.receiveMessage = receiveMessage;
		}

		@Override
		protected void doSend(String destination, Message<?> message) {
		}

		@Override
		protected Message<?> doReceive(String destination) {
			this.destination = destination;
			return this.receiveMessage;
		}

		@Override
		protected Message<?> doSendAndReceive(String destination, Message<?> requestMessage) {
			this.destination = destination;
			this.requestMessage = requestMessage;
			return this.receiveMessage;
		}
	}

}
