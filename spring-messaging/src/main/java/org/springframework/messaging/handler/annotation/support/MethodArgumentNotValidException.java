/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.handler.annotation.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.MethodArgumentResolutionException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;

/**
 * Exception to be thrown when a method argument fails validation perhaps as a
 * result of {@code @Valid} style validation, or perhaps because it is required.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0.1
 */
@SuppressWarnings("serial")
public class MethodArgumentNotValidException extends MethodArgumentResolutionException {

	private final @Nullable BindingResult bindingResult;


	/**
	 * Create a new instance with the invalid {@code MethodParameter}.
	 */
	public MethodArgumentNotValidException(Message<?> message, MethodParameter parameter) {
		super(message, parameter);
		this.bindingResult = null;
	}

	/**
	 * Create a new instance with the invalid {@code MethodParameter} and a
	 * {@link org.springframework.validation.BindingResult}.
	 */
	public MethodArgumentNotValidException(Message<?> message, MethodParameter parameter, BindingResult bindingResult) {
		super(message, parameter, getValidationErrorMessage(bindingResult));
		this.bindingResult = bindingResult;
	}


	/**
	 * Return the BindingResult if the failure is validation-related,
	 * or {@code null} if none.
	 */
	public final @Nullable BindingResult getBindingResult() {
		return this.bindingResult;
	}


	private static String getValidationErrorMessage(BindingResult bindingResult) {
		StringBuilder sb = new StringBuilder();
		sb.append(bindingResult.getErrorCount()).append(" error(s): ");
		for (ObjectError error : bindingResult.getAllErrors()) {
			sb.append('[').append(error).append("] ");
		}
		return sb.toString();
	}

}
