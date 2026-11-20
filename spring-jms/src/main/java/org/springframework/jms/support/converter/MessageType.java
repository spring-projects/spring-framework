/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.support.converter;

/**
 * Constants that indicate a target message type to convert to: a
 * {@link jakarta.jms.TextMessage}, a {@link jakarta.jms.BytesMessage},
 * a {@link jakarta.jms.MapMessage} or an {@link jakarta.jms.ObjectMessage}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see MarshallingMessageConverter#setTargetType
 */
public enum MessageType {

	/**
	 * A {@link jakarta.jms.TextMessage}.
	 */
	TEXT,

	/**
	 * A {@link jakarta.jms.BytesMessage}.
	 */
	BYTES,

	/**
	 * A {@link jakarta.jms.MapMessage}.
	 */
	MAP,

	/**
	 * A {@link jakarta.jms.ObjectMessage}.
	 */
	OBJECT

}
