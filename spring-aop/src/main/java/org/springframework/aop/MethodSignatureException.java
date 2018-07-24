package org.springframework.aop;

/**
 * A MethodSignatureException is thrown when afterThrowing that owns 4 params in ThrowsAdvice is invalid
 *
 * @author panlingxiao
 * @see ThrowsAdvice
 */
public class MethodSignatureException extends RuntimeException {

	/**
	 * Create an MethodSignatureException with a specific message.
	 *
	 * @param message the message
	 */
	public MethodSignatureException(String message) {
		super(message);
	}

	/**
	 * Create an MethodSignatureException with a specific message and cause.
	 *
	 * @param message the message
	 * @param cause   the cause
	 */
	public MethodSignatureException(String message, Exception cause) {
		super(message, cause);
	}
}
