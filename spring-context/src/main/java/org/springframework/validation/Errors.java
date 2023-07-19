/*
 * Copyright 2002-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.validation;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

/**
 * Stores and exposes information about data-binding and validation errors
 * for a specific object.
 *
 * <p>Field names are typically properties of the target object (e.g. "name"
 * when binding to a customer object). Implementations may also support nested
 * fields in case of nested objects (e.g. "address.street"), in conjunction
 * with subtree navigation via {@link #setNestedPath}: for example, an
 * {@code AddressValidator} may validate "address", not being aware that this
 * is a nested object of a top-level customer object.
 *
 * <p>Note: {@code Errors} objects are single-threaded.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see Validator
 * @see ValidationUtils
 * @see SimpleErrors
 * @see BindingResult
 */
public interface Errors {

	/**
	 * The separator between path elements in a nested path,
	 * for example in "customer.name" or "customer.address.street".
	 * <p>"." = same as the
	 * {@link org.springframework.beans.PropertyAccessor#NESTED_PROPERTY_SEPARATOR nested property separator}
	 * in the beans package.
	 */
	String NESTED_PATH_SEPARATOR = PropertyAccessor.NESTED_PROPERTY_SEPARATOR;


	/**
	 * Return the name of the bound root object.
	 */
	String getObjectName();

	/**
	 * Allow context to be changed so that standard validators can validate
	 * subtrees. Reject calls prepend the given path to the field names.
	 * <p>For example, an address validator could validate the subobject
	 * "address" of a customer object.
	 * <p>The default implementation throws {@code UnsupportedOperationException}
	 * since not all {@code Errors} implementations support nested paths.
	 * @param nestedPath nested path within this object,
	 * e.g. "address" (defaults to "", {@code null} is also acceptable).
	 * Can end with a dot: both "address" and "address." are valid.
	 * @see #getNestedPath()
	 */
	default void setNestedPath(String nestedPath) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support nested paths");
	}

	/**
	 * Return the current nested path of this {@link Errors} object.
	 * <p>Returns a nested path with a dot, i.e. "address.", for easy
	 * building of concatenated paths. Default is an empty String.
	 * @see #setNestedPath(String)
	 */
	default String getNestedPath() {
		return "";
	}

	/**
	 * Push the given sub path onto the nested path stack.
	 * <p>A {@link #popNestedPath()} call will reset the original
	 * nested path before the corresponding
	 * {@code pushNestedPath(String)} call.
	 * <p>Using the nested path stack allows to set temporary nested paths
	 * for subobjects without having to worry about a temporary path holder.
	 * <p>For example: current path "spouse.", pushNestedPath("child") &rarr;
	 * result path "spouse.child."; popNestedPath() &rarr; "spouse." again.
	 * <p>The default implementation throws {@code UnsupportedOperationException}
	 * since not all {@code Errors} implementations support nested paths.
	 * @param subPath the sub path to push onto the nested path stack
	 * @see #popNestedPath()
	 */
	default void pushNestedPath(String subPath) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support nested paths");
	}

	/**
	 * Pop the former nested path from the nested path stack.
	 * @throws IllegalStateException if there is no former nested path on the stack
	 * @see #pushNestedPath(String)
	 */
	default void popNestedPath() throws IllegalStateException {
		throw new IllegalStateException("Cannot pop nested path: no nested path on stack");
	}

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @see #reject(String, Object[], String)
	 */
	default void reject(String errorCode) {
		reject(errorCode, null, null);
	}

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @param defaultMessage fallback default message
	 * @see #reject(String, Object[], String)
	 */
	default void reject(String errorCode, String defaultMessage) {
		reject(errorCode, null, defaultMessage);
	}

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @param errorArgs error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 * @see #rejectValue(String, String, Object[], String)
	 */
	void reject(String errorCode, @Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * Register a field error for the specified field of the current object
	 * (respecting the current nested path, if any), using the given error
	 * description.
	 * <p>The field name may be {@code null} or empty String to indicate
	 * the current object itself rather than a field of it. This may result
	 * in a corresponding field error within the nested object graph or a
	 * global error if the current object is the top object.
	 * @param field the field name (may be {@code null} or empty String)
	 * @param errorCode error code, interpretable as a message key
	 * @see #rejectValue(String, String, Object[], String)
	 */
	default void rejectValue(@Nullable String field, String errorCode) {
		rejectValue(field, errorCode, null, null);
	}

	/**
	 * Register a field error for the specified field of the current object
	 * (respecting the current nested path, if any), using the given error
	 * description.
	 * <p>The field name may be {@code null} or empty String to indicate
	 * the current object itself rather than a field of it. This may result
	 * in a corresponding field error within the nested object graph or a
	 * global error if the current object is the top object.
	 * @param field the field name (may be {@code null} or empty String)
	 * @param errorCode error code, interpretable as a message key
	 * @param defaultMessage fallback default message
	 * @see #rejectValue(String, String, Object[], String)
	 */
	default void rejectValue(@Nullable String field, String errorCode, String defaultMessage) {
		rejectValue(field, errorCode, null, defaultMessage);
	}

	/**
	 * Register a field error for the specified field of the current object
	 * (respecting the current nested path, if any), using the given error
	 * description.
	 * <p>The field name may be {@code null} or empty String to indicate
	 * the current object itself rather than a field of it. This may result
	 * in a corresponding field error within the nested object graph or a
	 * global error if the current object is the top object.
	 * @param field the field name (may be {@code null} or empty String)
	 * @param errorCode error code, interpretable as a message key
	 * @param errorArgs error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
	 * @see #reject(String, Object[], String)
	 */
	void rejectValue(@Nullable String field, String errorCode,
			@Nullable Object[] errorArgs, @Nullable String defaultMessage);

	/**
	 * Add all errors from the given {@code Errors} instance to this
	 * {@code Errors} instance.
	 * <p>This is a convenience method to avoid repeated {@code reject(..)}
	 * calls for merging an {@code Errors} instance into another
	 * {@code Errors} instance.
	 * <p>Note that the passed-in {@code Errors} instance is supposed
	 * to refer to the same target object, or at least contain compatible errors
	 * that apply to the target object of this {@code Errors} instance.
	 * <p>The default implementation throws {@code UnsupportedOperationException}
	 * since not all {@code Errors} implementations support {@code #addAllErrors}.
	 * @param errors the {@code Errors} instance to merge in
	 * @see #getAllErrors()
	 */
	default void addAllErrors(Errors errors) {
		throw new UnsupportedOperationException(getClass().getSimpleName() + " does not support addAllErrors");
	}

	/**
	 * Throw the mapped exception with a message summarizing the recorded errors.
	 * @param messageToException a function mapping the message to the exception,
	 * e.g. {@code IllegalArgumentException::new} or {@code IllegalStateException::new}
	 * @param <T> the exception type to be thrown
	 * @since 6.1
	 * @see #toString()
	 */
	default <T extends Throwable> void failOnError(Function<String, T> messageToException) throws T {
		if (hasErrors()) {
			throw messageToException.apply(toString());
		}
	}

	/**
	 * Determine if there were any errors.
	 * @see #hasGlobalErrors()
	 * @see #hasFieldErrors()
	 */
	default boolean hasErrors() {
		return (!getGlobalErrors().isEmpty() || !getFieldErrors().isEmpty());
	}

	/**
	 * Determine the total number of errors.
	 * @see #getGlobalErrorCount()
	 * @see #getFieldErrorCount()
	 */
	default int getErrorCount() {
		return (getGlobalErrors().size() + getFieldErrors().size());
	}

	/**
	 * Get all errors, both global and field ones.
	 * @return a list of {@link ObjectError}/{@link FieldError} instances
	 * @see #getGlobalErrors()
	 * @see #getFieldErrors()
	 */
	default List<ObjectError> getAllErrors() {
		return Stream.concat(getGlobalErrors().stream(), getFieldErrors().stream()).toList();
	}

	/**
	 * Determine if there were any global errors.
	 * @see #hasFieldErrors()
	 */
	default boolean hasGlobalErrors() {
		return !getGlobalErrors().isEmpty();
	}

	/**
	 * Determine the number of global errors.
	 * @see #getFieldErrorCount()
	 */
	default int getGlobalErrorCount() {
		return getGlobalErrors().size();
	}

	/**
	 * Get all global errors.
	 * @return a list of {@link ObjectError} instances
	 * @see #getFieldErrors()
	 */
	List<ObjectError> getGlobalErrors();

	/**
	 * Get the <i>first</i> global error, if any.
	 * @return the global error, or {@code null}
	 * @see #getFieldError()
	 */
	@Nullable
	default ObjectError getGlobalError() {
		return getGlobalErrors().stream().findFirst().orElse(null);
	}

	/**
	 * Determine if there were any errors associated with a field.
	 * @see #hasGlobalErrors()
	 */
	default boolean hasFieldErrors() {
		return !getFieldErrors().isEmpty();
	}

	/**
	 * Determine the number of errors associated with a field.
	 * @see #getGlobalErrorCount()
	 */
	default int getFieldErrorCount() {
		return getFieldErrors().size();
	}

	/**
	 * Get all errors associated with a field.
	 * @return a List of {@link FieldError} instances
	 * @see #getGlobalErrors()
	 */
	List<FieldError> getFieldErrors();

	/**
	 * Get the <i>first</i> error associated with a field, if any.
	 * @return the field-specific error, or {@code null}
	 * @see #getGlobalError()
	 */
	@Nullable
	default FieldError getFieldError() {
		return getFieldErrors().stream().findFirst().orElse(null);
	}

	/**
	 * Determine if there were any errors associated with the given field.
	 * @param field the field name
	 * @see #hasFieldErrors()
	 */
	default boolean hasFieldErrors(String field) {
		return (getFieldError(field) != null);
	}

	/**
	 * Determine the number of errors associated with the given field.
	 * @param field the field name
	 * @see #getFieldErrorCount()
	 */
	default int getFieldErrorCount(String field) {
		return getFieldErrors(field).size();
	}

	/**
	 * Get all errors associated with the given field.
	 * <p>Implementations may support not only full field names like
	 * "address.street" but also pattern matches like "address.*".
	 * @param field the field name
	 * @return a List of {@link FieldError} instances
	 * @see #getFieldErrors()
	 */
	default List<FieldError> getFieldErrors(String field) {
		return getFieldErrors().stream().filter(error -> field.equals(error.getField())).toList();
	}

	/**
	 * Get the first error associated with the given field, if any.
	 * @param field the field name
	 * @return the field-specific error, or {@code null}
	 * @see #getFieldError()
	 */
	@Nullable
	default FieldError getFieldError(String field) {
		return getFieldErrors().stream().filter(error -> field.equals(error.getField())).findFirst().orElse(null);
	}

	/**
	 * Return the current value of the given field, either the current
	 * bean property value or a rejected update from the last binding.
	 * <p>Allows for convenient access to user-specified field values,
	 * even if there were type mismatches.
	 * @param field the field name
	 * @return the current value of the given field
	 * @see #getFieldType(String)
	 */
	@Nullable
	Object getFieldValue(String field);

	/**
	 * Determine the type of the given field, as far as possible.
	 * <p>Implementations should be able to determine the type even
	 * when the field value is {@code null}, for example from some
	 * associated descriptor.
	 * @param field the field name
	 * @return the type of the field, or {@code null} if not determinable
	 * @see #getFieldValue(String)
	 */
	@Nullable
	default Class<?> getFieldType(String field) {
		return Optional.ofNullable(getFieldValue(field)).map(Object::getClass).orElse(null);
	}

	/**
	 * Return a summary of the recorded errors,
	 * e.g. for inclusion in an exception message.
	 * @see #failOnError(Function)
	 */
	String toString();

}
