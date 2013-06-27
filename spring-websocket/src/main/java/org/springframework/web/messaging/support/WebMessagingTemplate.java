package org.springframework.web.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.converter.DefaultMessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.core.MessagePostProcessor;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.web.messaging.MessageType;


public class WebMessagingTemplate implements MessageSendingOperations<String> {

	private final MessageChannel outputChannel;

	protected volatile MessageConverter converter = new DefaultMessageConverter();

	private volatile String defaultDestination;

	private volatile long sendTimeout = -1;


	public WebMessagingTemplate(MessageChannel outputChannel) {
		Assert.notNull(outputChannel, "outputChannel is required");
		this.outputChannel = outputChannel;
	}


	/**
	 * Set the {@link MessageConverter} that is to be used to convert
	 * between Messages and objects for this template.
	 * <p>The default is {@link SimpleMessageConverter}.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "'messageConverter' must not be null");
		this.converter = messageConverter;
	}

	/**
	 * Specify the timeout value to use for send operations.
	 *
	 * @param sendTimeout the send timeout in milliseconds
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setDefaultDestination(String defaultDestination) {
		this.defaultDestination = defaultDestination;
	}


	@Override
	public <P> void send(Message<P> message) {
		this.send(getRequiredDefaultDestination(), message);
	}

	private String getRequiredDefaultDestination() {

		// TODO: maybe look up destination of current message (via ThreadLocal)

		Assert.state(this.defaultDestination != null,
				"No 'defaultDestination' specified for WebMessagingTemplate. " +
				"Unable to invoke method without an explicit destination argument.");

		return this.defaultDestination;
	}

	@Override
	public <P> void send(String destinationName, Message<P> message) {
		Assert.notNull(destinationName, "destinationName is required");
		message = addDestinationToMessage(message, destinationName);
		long timeout = this.sendTimeout;
		boolean sent = (timeout >= 0)
				? this.outputChannel.send(message, timeout)
				: this.outputChannel.send(message);
		if (!sent) {
			throw new MessageDeliveryException(message,
					"failed to send message to destination '" + destinationName + "' within timeout: " + timeout);
		}
	}

	protected <P> Message<P> addDestinationToMessage(Message<P> message, String destinationName) {
		Assert.notNull(destinationName, "destinationName is required");
		WebMessageHeaderAccesssor headers = WebMessageHeaderAccesssor.create(MessageType.MESSAGE);
		headers.copyHeaders(message.getHeaders());
		headers.setDestination(destinationName);
		message = MessageBuilder.withPayload(message.getPayload()).copyHeaders(headers.toMap()).build();
		return message;
	}

	@Override
	public <T> void convertAndSend(T object) {
		this.convertAndSend(getRequiredDefaultDestination(), object);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T object) {
		this.convertAndSend(destinationName, object, null);
	}

	@Override
	public <T> void convertAndSend(T object, MessagePostProcessor postProcessor) {
		this.convertAndSend(getRequiredDefaultDestination(), object, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T object, MessagePostProcessor postProcessor) {
		Message<?> message = this.converter.toMessage(object);
		if (postProcessor != null) {
			message = postProcessor.postProcessMessage(message);
		}
		this.send(destinationName, message);
	}

}
