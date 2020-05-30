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

import java.io.Writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionFailedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.GenericMessageConverter;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for receiving operations in {@link AbstractMessagingTemplate}.
 *
 * @author Rossen Stoyanchev
 * @see MessageRequestReplyTemplateTests
 */
public class MessageReceivingTemplateTests {

	private TestMessagingTemplate template;


	@BeforeEach
	public void setup() {
		this.template = new TestMessagingTemplate();
	}

	@Test
	public void receive() {
		Message<?> expected = new GenericMessage<>("payload");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(expected);
		Message<?> actual = this.template.receive();

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(actual).isSameAs(expected);
	}

	@Test
	public void receiveMissingDefaultDestination() {
		assertThatIllegalStateException().isThrownBy(
				this.template::receive);
	}

	@Test
	public void receiveFromDestination() {
		Message<?> expected = new GenericMessage<>("payload");
		this.template.setReceiveMessage(expected);
		Message<?> actual = this.template.receive("somewhere");

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(actual).isSameAs(expected);
	}

	@Test
	public void receiveAndConvert() {
		Message<?> expected = new GenericMessage<>("payload");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(expected);
		String payload = this.template.receiveAndConvert(String.class);

		assertThat(this.template.destination).isEqualTo("home");
		assertThat(payload).isSameAs("payload");
	}

	@Test
	public void receiveAndConvertFromDestination() {
		Message<?> expected = new GenericMessage<>("payload");
		this.template.setReceiveMessage(expected);
		String payload = this.template.receiveAndConvert("somewhere", String.class);

		assertThat(this.template.destination).isEqualTo("somewhere");
		assertThat(payload).isSameAs("payload");
	}

	@Test
	public void receiveAndConvertFailed() {
		Message<?> expected = new GenericMessage<>("not a number test");
		this.template.setReceiveMessage(expected);
		this.template.setMessageConverter(new GenericMessageConverter());

		assertThatExceptionOfType(MessageConversionException.class).isThrownBy(() ->
				this.template.receiveAndConvert("somewhere", Integer.class))
			.withCauseInstanceOf(ConversionFailedException.class);
	}

	@Test
	public void receiveAndConvertNoConverter() {
		Message<?> expected = new GenericMessage<>("payload");
		this.template.setDefaultDestination("home");
		this.template.setReceiveMessage(expected);
		this.template.setMessageConverter(new GenericMessageConverter());
		try {
			this.template.receiveAndConvert(Writer.class);
		}
		catch (MessageConversionException ex) {
			assertThat(ex.getMessage().contains("payload")).as("Invalid exception message '" + ex.getMessage() + "'").isTrue();
			assertThat(ex.getFailedMessage()).isSameAs(expected);
		}
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
