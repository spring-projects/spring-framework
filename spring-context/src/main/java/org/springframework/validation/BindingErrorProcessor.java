/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation;

import org.springframework.beans.PropertyAccessException;

/**
 * Strategy for processing {@code DataBinder}'s missing field errors,
 * and for translating a {@code PropertyAccessException} to a
 * {@code FieldError}.
 *
 * <p>The error processor is pluggable so you can treat errors differently
 * if you want to. A default implementation is provided for typical needs.
 *
 * <p>Note: As of Spring 2.0, this interface operates on a given BindingResult,
 * to be compatible with any binding strategy (bean property, direct field access, etc).
 * It can still receive a BindException as argument (since a BindException implements
 * the BindingResult interface as well) but no longer operates on it directly.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @since 1.2
 * @see DataBinder#setBindingErrorProcessor
 * @see DefaultBindingErrorProcessor
 * @see BindingResult
 * @see BindException
 */
public interface BindingErrorProcessor {

	/**
	 * Apply the missing field error to the given BindException.
	 * <p>Usually, a field error is created for a missing required field.
	 * @param missingField the field that was missing during binding
	 * @param bindingResult the errors object to add the error(s) to.
	 * You can add more than just one error or maybe even ignore it.
	 * The {@code BindingResult} object features convenience utils such as
	 * a {@code resolveMessageCodes} method to resolve an error code.
	 * @see BeanPropertyBindingResult#addError
	 * @see BeanPropertyBindingResult#resolveMessageCodes
	 */
	void processMissingFieldError(String missingField, BindingResult bindingResult);

	/**
	 * Translate the given {@code PropertyAccessException} to an appropriate
	 * error registered on the given {@code Errors} instance.
	 * <p>Note that two error types are available: {@code FieldError} and
	 * {@code ObjectError}. Usually, field errors are created, but in certain
	 * situations one might want to create a global {@code ObjectError} instead.
	 * @param ex the {@code PropertyAccessException} to translate
	 * @param bindingResult the errors object to add the error(s) to.
	 * You can add more than just one error or maybe even ignore it.
	 * The {@code BindingResult} object features convenience utils such as
	 * a {@code resolveMessageCodes} method to resolve an error code.
	 * @see Errors
	 * @see FieldError
	 * @see ObjectError
	 * @see MessageCodesResolver
	 * @see BeanPropertyBindingResult#addError
	 * @see BeanPropertyBindingResult#resolveMessageCodes
	 */
	void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult);

}
