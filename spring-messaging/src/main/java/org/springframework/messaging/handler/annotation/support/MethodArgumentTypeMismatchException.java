/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.support;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;

/**
 * Exception that indicates that a method argument has not the expected type.
 *
 * @author Stephane Nicoll
 * @since 4.0.3
 */
@SuppressWarnings("serial")
public class MethodArgumentTypeMismatchException extends MethodArgumentResolutionException {

	public MethodArgumentTypeMismatchException(Message<?> message, MethodParameter parameter, String description) {
		super(message, parameter, description);
	}

}
