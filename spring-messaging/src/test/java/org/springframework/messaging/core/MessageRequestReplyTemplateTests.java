/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.junit.Assert.*;

/**
 * Unit tests for request and reply operations in {@link AbstractMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 *
 * @see MessageReceivingTemplateTests
 */
public class MessageRequestReplyTemplateTests {

	private TestMessagingTemplate template;

	private TestMessagePostProcessor postProcessor;

	private Map<String, Object> headers;


	@Before
	public void setup() {
		this.template = new TestMessagingTemplate();
		this.postProcessor = new TestMessagePostProcessor();
		this.headers = Collections.<String, Object>singletonMap("key", "value");
	}


	@Test
	public void sendAndReceive() {
		Message<?> requestMessage = new GenericMessage<Object>("request");
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(responseMessage);
		Message<?> actual = this.template.sendAndReceive(requestMessage);

		assertEquals("home", this.template.destination);
		assertSame(requestMessage, this.template.requestMessage);
		assertSame(responseMessage, actual);
	}

	@Test(expected = IllegalStateException.class)
	public void sendAndReceiveMissingDestination() {
		this.template.sendAndReceive(new GenericMessage<Object>("request"));
	}

	@Test
	public void sendAndReceiveToDestination() {
		Message<?> requestMessage = new GenericMessage<Object>("request");
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		Message<?> actual = this.template.sendAndReceive("somewhere", requestMessage);

		assertEquals("somewhere", this.template.destination);
		assertSame(requestMessage, this.template.requestMessage);
		assertSame(responseMessage, actual);
	}

	@Test
	public void convertAndSend() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("request", String.class);

		assertEquals("home", this.template.destination);
		assertSame("request", this.template.requestMessage.getPayload());
		assertSame("response", response);
	}

	@Test
	public void convertAndSendToDestination() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", String.class);

		assertEquals("somewhere", this.template.destination);
		assertSame("request", this.template.requestMessage.getPayload());
		assertSame("response", response);
	}

	@Test
	public void convertAndSendToDestinationWithHeaders() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", this.headers, String.class);

		assertEquals("somewhere", this.template.destination);
		assertEquals("value", this.template.requestMessage.getHeaders().get("key"));
		assertSame("request", this.template.requestMessage.getPayload());
		assertSame("response", response);
	}

	@Test
	public void convertAndSendWithPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("request", String.class, this.postProcessor);

		assertEquals("home", this.template.destination);
		assertSame("request", this.template.requestMessage.getPayload());
		assertSame("response", response);
		assertSame(this.postProcessor.getMessage(), this.template.requestMessage);
	}

	@Test
	public void convertAndSendToDestinationWithPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", String.class, this.postProcessor);

		assertEquals("somewhere", this.template.destination);
		assertSame("request", this.template.requestMessage.getPayload());
		assertSame("response", response);
		assertSame(this.postProcessor.getMessage(), this.template.requestMessage);
	}

	@Test
	public void convertAndSendToDestinationWithHeadersAndPostProcessor() {
		Message<?> responseMessage = new GenericMessage<Object>("response");
		this.template.setReceiveMessage(responseMessage);
		String response = this.template.convertSendAndReceive("somewhere", "request", this.headers,
				String.class, this.postProcessor);

		assertEquals("somewhere", this.template.destination);
		assertEquals("value", this.template.requestMessage.getHeaders().get("key"));
		assertSame("request", this.template.requestMessage.getPayload());
		assertSame("response", response);
		assertSame(this.postProcessor.getMessage(), this.template.requestMessage);
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
