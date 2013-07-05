package org.springframework.web.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.messaging.MessageType;


public class WebMessagingTemplate extends AbstractMessageSendingTemplate<String> {

	private final MessageChannel outputChannel;

	private volatile long sendTimeout = -1;


	public WebMessagingTemplate(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel is required");
		this.outputChannel = outputChannel;
	}


	/**
	 * Specify the timeout value to use for send operations.
	 *
	 * @param sendTimeout the send timeout in milliseconds
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}


	@Override
	public <P> void send(Message<P> message) {
		// TODO: maybe look up destination of current message (via ThreadLocal)
		this.send(getRequiredDefaultDestination(), message);
	}

	@Override
	protected void doSend(String destination, Message<?> message) {
		Assert.notNull(destination, "destination is required");
		message = addDestinationToMessage(message, destination);
		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0)
				? this.outputChannel.send(message, timeout)
				: this.outputChannel.send(message);
		if (!sent) {
			throw new MessageDeliveryException(message,
					"failed to send message to destination '" + destination + "' within timeout: " + timeout);
		}
	}

	protected <P> Message<P> addDestinationToMessage(Message<P> message, String destination) {
		Assert.notNull(destination, "destination is required");
		WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.create(MessageType.MESSAGE);
		headers.copyHeaders(message.getHeaders());
		headers.setDestination(destination);
		message = MessageBuilder.withPayload(message.getPayload()).copyHeaders(headers.toMap()).build();
		return message;
	}

}
