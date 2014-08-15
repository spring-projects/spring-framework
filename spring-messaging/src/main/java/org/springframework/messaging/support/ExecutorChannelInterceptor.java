package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

/**
 * An extension of {@link ChannelInterceptor} with callbacks to intercept the
 * asynchronous sending of a {@link org.springframework.messaging.Message} to a
 * specific subscriber through an {@link java.util.concurrent.Executor}.
 * Supported on {@link org.springframework.messaging.MessageChannel}
 * implementations that can be configured with an Executor.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface ExecutorChannelInterceptor extends ChannelInterceptor {

	/**
	 * Invoked inside the {@link Runnable} submitted to the Executor just before
	 * calling the target MessageHandler to handle the message. Allows for
	 * modification of the Message if necessary or when {@code null} is returned
	 * the MessageHandler is not invoked.
	 *
	 * @param message the message to be handled
	 * @param channel the channel on which the message was sent to
	 * @param handler the target handler to handle the message
	 * @return the input message, or a new instance, or {@code null}
	 */
	Message<?> beforeHandle(Message<?> message, MessageChannel channel, MessageHandler handler);

	/**
	 * Invoked inside the {@link Runnable} submitted to the Executor after calling
	 * the target MessageHandler regardless of the outcome (i.e. Exception raised
	 * or not) thus allowing for proper resource cleanup.
	 *
	 * <p>Note that this will be invoked only if beforeHandle successfully completed
	 * and returned a Message, i.e. it did not return {@code null}.
	 *
	 * @param message the message handled
	 * @param channel the channel on which the message was sent to
	 * @param handler the target handler that handled the message
	 * @param ex any exception that may been raised by the handler
	 */
	void afterMessageHandled(Message<?> message, MessageChannel channel, MessageHandler handler, Exception ex);

}
