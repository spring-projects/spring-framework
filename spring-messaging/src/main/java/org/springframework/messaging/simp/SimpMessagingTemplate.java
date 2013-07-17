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
package org.springframework.messaging.simp;

import java.util.Arrays;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;


/**
 * A specialization of {@link AbstractMessageSendingTemplate} that adds String-based
 * destinations as a message header.
 *
 * @author Mark Fisher
 * @since 4.0
 */
public class SimpMessagingTemplate extends AbstractMessageSendingTemplate<String>
		implements SimpMessageSendingOperations {

	private final MessageChannel messageChannel;

	private String userDestinationPrefix = "/user/";

	private volatile long sendTimeout = -1;


	public SimpMessagingTemplate(MessageChannel messageChannel) {
		Assert.notNull(messageChannel, "outputChannel is required");
		this.messageChannel = messageChannel;
	}


	/**
	 * Configure the prefix to use for destinations targeting a specific user.
	 * <p>The default value is "/user/".
	 * @see org.springframework.messaging.simp.handler.UserDestinationMessageHandler
	 */
	public void setUserDestinationPrefix(String prefix) {
		this.userDestinationPrefix = prefix;
	}

	/**
	 * @return the userDestinationPrefix
	 */
	public String getUserDestinationPrefix() {
		return this.userDestinationPrefix;
	}

	/**
	 * @return the messageChannel
	 */
	public MessageChannel getMessageChannel() {
		return this.messageChannel;
	}

	/**
	 * Specify the timeout value to use for send operations.
	 *
	 * @param sendTimeout the send timeout in milliseconds
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * @return the sendTimeout
	 */
	public long getSendTimeout() {
		return this.sendTimeout;
	}


	@Override
	public <P> void send(Message<P> message) {
		// TODO: maybe look up destination of current message (via ThreadLocal)
		this.send(getRequiredDefaultDestination(), message);
	}

	@Override
	protected void doSend(String destination, Message<?> message) {
		Assert.notNull(destination, "destination is required");
		message = updateMessageHeaders(message, destination);
		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0)
				? this.messageChannel.send(message, timeout)
				: this.messageChannel.send(message);
		if (!sent) {
			throw new MessageDeliveryException(message,
					"failed to send message to destination '" + destination + "' within timeout: " + timeout);
		}
	}

	protected <P> Message<P> updateMessageHeaders(Message<P> message, String destination) {
		Assert.notNull(destination, "destination is required");
		return MessageBuilder.fromMessage(message)
				.setHeader(SimpMessageHeaderAccessor.MESSAGE_TYPE, SimpMessageType.MESSAGE)
				.setHeader(SimpMessageHeaderAccessor.DESTINATIONS, Arrays.asList(destination)).build();
	}

	@Override
	public <T> void convertAndSendToUser(String user, String destination, T message) throws MessagingException {
		convertAndSendToUser(user, destination, message, null);
	}

	@Override
	public <T> void convertAndSendToUser(String user, String destination, T message,
			MessagePostProcessor postProcessor) throws MessagingException {

		Assert.notNull(user, "user is required");
		convertAndSend(this.userDestinationPrefix + user + destination, message, postProcessor);
	}

}
