/*
 * Copyright 2002-2006 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation;

import org.springframework.beans.PropertyAccessException;

/**
 * Strategy for processing <code>DataBinder</code>'s missing field errors,
 * and for translating a <code>PropertyAccessException</code> to a
 * <code>FieldError</code>.
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
	 * The <code>BindingResult</code> object features convenience utils such as
	 * a <code>resolveMessageCodes</code> method to resolve an error code.
	 * @see BeanPropertyBindingResult#addError
	 * @see BeanPropertyBindingResult#resolveMessageCodes
	 */
	void processMissingFieldError(String missingField, BindingResult bindingResult);

	/**
	 * Translate the given <code>PropertyAccessException</code> to an appropriate
	 * error registered on the given <code>Errors</code> instance.
	 * <p>Note that two error types are available: <code>FieldError</code> and
	 * <code>ObjectError</code>. Usually, field errors are created, but in certain
	 * situations one might want to create a global <code>ObjectError</code> instead.
	 * @param ex the <code>PropertyAccessException</code> to translate
	 * @param bindingResult the errors object to add the error(s) to.
	 * You can add more than just one error or maybe even ignore it.
	 * The <code>BindingResult</code> object features convenience utils such as
	 * a <code>resolveMessageCodes</code> method to resolve an error code.
	 * @see Errors
	 * @see FieldError
	 * @see ObjectError
	 * @see MessageCodesResolver
	 * @see BeanPropertyBindingResult#addError
	 * @see BeanPropertyBindingResult#resolveMessageCodes
	 */
	void processPropertyAccessException(PropertyAccessException ex, BindingResult bindingResult);

}
