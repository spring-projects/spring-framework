/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

/**
 * A {@link MessageChannel} that maintains a registry of subscribers and invokes
 * them to handle messages sent through this channel.
 *
 * @author Mark Fisher
 * @since 4.0
 */
public interface SubscribableChannel extends MessageChannel {

	/**
	 * Register a message handler.
	 * @return {@code true} if the handler was subscribed or {@code false} if it
	 * was already subscribed.
	 */
	boolean subscribe(MessageHandler handler);

	/**
	 * Un-register a message handler.
	 * @return {@code true} if the handler was un-registered, or {@code false}
	 * if was not registered.
	 */
	boolean unsubscribe(MessageHandler handler);

}
