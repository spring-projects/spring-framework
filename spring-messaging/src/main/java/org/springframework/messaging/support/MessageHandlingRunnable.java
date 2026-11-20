/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * Extension of the {@link Runnable} interface with methods to obtain the
 * {@link MessageHandler} and {@link Message} to be handled.
 *
 * @author Rossen Stoyanchev
 * @since 4.1.1
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
