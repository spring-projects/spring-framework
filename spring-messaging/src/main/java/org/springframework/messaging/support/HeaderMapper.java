/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.support;

import org.springframework.messaging.MessageHeaders;

/**
 * Generic strategy interface for mapping {@link MessageHeaders} to and from other
 * types of objects. This would typically be used by adapters where the "other type"
 * has a concept of headers or properties (HTTP, JMS, AMQP, etc).
 *
 * @author Mark Fisher
 * @since 4.1
 * @param <T> type of the instance to and from which headers will be mapped
 */
public interface HeaderMapper<T> {

	/**
	 * Map from the given {@link MessageHeaders} to the specified target message.
	 * @param headers the abstracted MessageHeaders
	 * @param target the native target message
	 */
	void fromHeaders(MessageHeaders headers, T target);

	/**
	 * Map from the given target message to abstracted {@link MessageHeaders}.
	 * @param source the native target message
	 * @return the abstracted MessageHeaders
	 */
	MessageHeaders toHeaders(T source);

}
