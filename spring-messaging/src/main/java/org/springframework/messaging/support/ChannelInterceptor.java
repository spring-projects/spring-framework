/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

/**
 * Interface for interceptors that are able to view and/or modify the
 * {@link Message Messages} being sent-to and/or received-from a
 * {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see Message
 * @see MessageChannel
 */
public interface ChannelInterceptor {

	/**
	 * Invoked before the Message is actually sent to the channel.
	 * This allows for modification of the Message if necessary.
	 * If this method returns {@code null} then the actual
	 * send invocation will not occur.
	 */
	default @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
		return message;
	}

	/**
	 * Invoked immediately after the send invocation. The boolean
	 * value argument represents the return value of that invocation.
	 */
	default void postSend(Message<?> message, MessageChannel channel, boolean sent) {
	}

	/**
	 * Invoked after the completion of a send regardless of any exception that
	 * have been raised thus allowing for proper resource cleanup.
	 * <p>Note that this will be invoked only if {@link #preSend} successfully
	 * completed and returned a Message, i.e. it did not return {@code null}.
	 * @since 4.1
	 */
	default void afterSendCompletion(
			Message<?> message, MessageChannel channel, boolean sent, @Nullable Exception ex) {
	}

	/**
	 * Invoked as soon as receive is called and before a Message is
	 * actually retrieved. If the return value is 'false', then no
	 * Message will be retrieved. This only applies to PollableChannels.
	 */
	default boolean preReceive(MessageChannel channel) {
		return true;
	}

	/**
	 * Invoked immediately after a Message has been retrieved but before
	 * it is returned to the caller. The Message may be modified if
	 * necessary; {@code null} aborts further interceptor invocations.
	 * This only applies to PollableChannels.
	 */
	default @Nullable Message<?> postReceive(Message<?> message, MessageChannel channel) {
		return message;
	}

	/**
	 * Invoked after the completion of a receive regardless of any exception that
	 * have been raised thus allowing for proper resource cleanup.
	 * <p>Note that this will be invoked only if {@link #preReceive} successfully
	 * completed and returned {@code true}.
	 * @since 4.1
	 */
	default void afterReceiveCompletion(@Nullable Message<?> message, MessageChannel channel,
			@Nullable Exception ex) {
	}

}
