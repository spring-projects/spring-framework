/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging.core;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Unit tests for receiving operations in {@link AbstractMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 *
 * @see MessageRequestReplyTemplateTests
 */
public class MessageReceivingTemplateTests {

	private TestMessagingTemplate template;


	@Before
	public void setup() {
		this.template = new TestMessagingTemplate();
	}

	@Test
	public void receive() {
		Message<?> expected = new GenericMessage<Object>("payload");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(expected);
		Message<?> actual = this.template.receive();

		assertEquals("home", this.template.destination);
		assertSame(expected, actual);
	}

	@Test(expected = IllegalStateException.class)
	public void receiveMissingDefaultDestination() {
		this.template.receive();
	}

	@Test
	public void receiveFromDestination() {
		Message<?> expected = new GenericMessage<Object>("payload");
		this.template.setReceiveMessage(expected);
		Message<?> actual = this.template.receive("somewhere");

		assertEquals("somewhere", this.template.destination);
		assertSame(expected, actual);
	}

	@Test
	public void receiveAndConvert() {
		Message<?> expected = new GenericMessage<Object>("payload");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(expected);
		String payload = this.template.receiveAndConvert(String.class);

		assertEquals("home", this.template.destination);
		assertSame("payload", payload);
	}

	@Test
	public void receiveAndConvertFromDestination() {
		Message<?> expected = new GenericMessage<Object>("payload");
		this.template.setReceiveMessage(expected);
		String payload = this.template.receiveAndConvert("somewhere", String.class);

		assertEquals("somewhere", this.template.destination);
		assertSame("payload", payload);
	}



	private static class TestMessagingTemplate extends AbstractMessagingTemplate<String> {

		private String destination;

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
			return null;
		}

	}

}
