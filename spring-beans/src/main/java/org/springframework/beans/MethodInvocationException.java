/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import java.beans.PropertyChangeEvent;

import org.jspecify.annotations.Nullable;

/**
 * Thrown when a bean property getter or setter method throws an exception,
 * analogous to an InvocationTargetException.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
@SuppressWarnings("serial")
public class MethodInvocationException extends PropertyAccessException {

	/**
	 * Error code that a method invocation error will be registered with.
	 */
	public static final String ERROR_CODE = "methodInvocation";


	/**
	 * Create a new MethodInvocationException.
	 * @param propertyChangeEvent the PropertyChangeEvent that resulted in an exception
	 * @param cause the Throwable raised by the invoked method
	 */
	public MethodInvocationException(PropertyChangeEvent propertyChangeEvent, @Nullable Throwable cause) {
		super(propertyChangeEvent,
				"Property '" + propertyChangeEvent.getPropertyName() + "' threw exception: " + cause,
				cause);
	}

	@Override
	public String getErrorCode() {
		return ERROR_CODE;
	}

}
