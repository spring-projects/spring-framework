package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Extension of the Runnable interface with methods to obtain the MessageHandler
 * and Message to be handled.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public interface MessageHandlingRunnable extends Runnable {

	/**
	 * Return the Message that will be handled.
	 */
	Message<?> getMessage();

	/**
	 * Return the MessageHandler that will be used to handle the message.
	 */
	MessageHandler getMessageHandler();

}
