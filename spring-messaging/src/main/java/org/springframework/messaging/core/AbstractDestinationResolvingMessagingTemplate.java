/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.core;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * An extension of {@link AbstractMessagingTemplate} that adds operations for sending
 * messages to a resolvable destination name. Supports destination resolving as defined by
 * the following interfaces:
 * <ul>
 * <li>{@link DestinationResolvingMessageSendingOperations}</li>
 * <li>{@link DestinationResolvingMessageReceivingOperations}</li>
 * <li>{@link DestinationResolvingMessageRequestReplyOperations}</li>
 * </ul>
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @param <D> the destination type
 */
public abstract class AbstractDestinationResolvingMessagingTemplate<D> extends AbstractMessagingTemplate<D>
		implements DestinationResolvingMessageSendingOperations<D>,
		DestinationResolvingMessageReceivingOperations<D>,
		DestinationResolvingMessageRequestReplyOperations<D> {

	private @Nullable DestinationResolver<D> destinationResolver;


	/**
	 * Configure the {@link DestinationResolver} to use to resolve String destination
	 * names into actual destinations of type {@code <D>}.
	 * <p>This field does not have a default setting. If not configured, methods that
	 * require resolving a destination name will raise an {@link IllegalArgumentException}.
	 * @param destinationResolver the destination resolver to use
	 */
	public void setDestinationResolver(@Nullable DestinationResolver<D> destinationResolver) {
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Return the configured destination resolver.
	 */
	public @Nullable DestinationResolver<D> getDestinationResolver() {
		return this.destinationResolver;
	}

	protected final D resolveDestination(String destinationName) throws DestinationResolutionException {
		Assert.state(this.destinationResolver != null,
				"DestinationResolver is required to resolve destination names");
		return this.destinationResolver.resolveDestination(destinationName);
	}


	@Override
	public void send(String destinationName, Message<?> message) throws MessagingException {
		D destination = resolveDestination(destinationName);
		doSend(destination, message);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload) throws MessagingException {
		convertAndSend(destinationName, payload, null, null);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, @Nullable Map<String, Object> headers)
			throws MessagingException {

		convertAndSend(destinationName, payload, headers, null);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, @Nullable MessagePostProcessor postProcessor)
			throws MessagingException {

		convertAndSend(destinationName, payload, null, postProcessor);
	}

	@Override
	public <T> void convertAndSend(String destinationName, T payload, @Nullable Map<String, Object> headers,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {

		D destination = resolveDestination(destinationName);
		super.convertAndSend(destination, payload, headers, postProcessor);
	}

	@Override
	public @Nullable Message<?> receive(String destinationName) throws MessagingException {
		D destination = resolveDestination(destinationName);
		return super.receive(destination);
	}

	@Override
	public <T> @Nullable T receiveAndConvert(String destinationName, Class<T> targetClass) throws MessagingException {
		D destination = resolveDestination(destinationName);
		return super.receiveAndConvert(destination, targetClass);
	}

	@Override
	public @Nullable Message<?> sendAndReceive(String destinationName, Message<?> requestMessage)
			throws MessagingException {

		D destination = resolveDestination(destinationName);
		return super.sendAndReceive(destination, requestMessage);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass)
			throws MessagingException {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, targetClass);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(String destinationName, Object request,
			@Nullable Map<String, Object> headers, Class<T> targetClass) throws MessagingException {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, headers, targetClass);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(String destinationName, Object request, Class<T> targetClass,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, targetClass, postProcessor);
	}

	@Override
	public <T> @Nullable T convertSendAndReceive(String destinationName, Object request,
			@Nullable Map<String, Object> headers, Class<T> targetClass,
			@Nullable MessagePostProcessor postProcessor) throws MessagingException {

		D destination = resolveDestination(destinationName);
		return super.convertSendAndReceive(destination, request, headers, targetClass, postProcessor);
	}

}
