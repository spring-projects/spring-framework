/*
 * Copyright 2002-2018 the original author or authors.
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

import org.springframework.beans.PropertyAccessor;
import org.springframework.lang.Nullable;

/**
 * Stores and exposes information about data-binding and validation
 * errors for a specific object.
 *
 * <p>Field names can be properties of the target object (e.g. "name"
 * when binding to a customer object), or nested fields in case of
 * subobjects (e.g. "address.street"). Supports subtree navigation
 * via {@link #setNestedPath(String)}: for example, an
 * {@code AddressValidator} validates "address", not being aware
 * that this is a subobject of customer.
 *
 * <p>Note: {@code Errors} objects are single-threaded.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setNestedPath
 * @see BindException
 * @see DataBinder
 * @see ValidationUtils
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
	 * @param nestedPath nested path within this object,
	 * e.g. "address" (defaults to "", {@code null} is also acceptable).
	 * Can end with a dot: both "address" and "address." are valid.
	 */
	void setNestedPath(String nestedPath);

	/**
	 * Return the current nested path of this {@link Errors} object.
	 * <p>Returns a nested path with a dot, i.e. "address.", for easy
	 * building of concatenated paths. Default is an empty String.
	 */
	String getNestedPath();

	/**
	 * Push the given sub path onto the nested path stack.
	 * <p>A {@link #popNestedPath()} call will reset the original
	 * nested path before the corresponding
	 * {@code pushNestedPath(String)} call.
	 * <p>Using the nested path stack allows to set temporary nested paths
	 * for subobjects without having to worry about a temporary path holder.
	 * <p>For example: current path "spouse.", pushNestedPath("child") &rarr;
	 * result path "spouse.child."; popNestedPath() &rarr; "spouse." again.
	 * @param subPath the sub path to push onto the nested path stack
	 * @see #popNestedPath
	 */
	void pushNestedPath(String subPath);

	/**
	 * Pop the former nested path from the nested path stack.
	 * @throws IllegalStateException if there is no former nested path on the stack
	 * @see #pushNestedPath
	 */
	void popNestedPath() throws IllegalStateException;

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 */
	void reject(String errorCode);

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @param defaultMessage fallback default message
	 */
	void reject(String errorCode, String defaultMessage);

	/**
	 * Register a global error for the entire target object,
	 * using the given error description.
	 * @param errorCode error code, interpretable as a message key
	 * @param errorArgs error arguments, for argument binding via MessageFormat
	 * (can be {@code null})
	 * @param defaultMessage fallback default message
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
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode);

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
	 * @see #getNestedPath()
	 */
	void rejectValue(@Nullable String field, String errorCode, String defaultMessage);

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
	 * @see #getNestedPath()
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
	 * @param errors the {@code Errors} instance to merge in
	 */
	void addAllErrors(Errors errors);

	/**
	 * Return if there were any errors.
	 */
	boolean hasErrors();

	/**
	 * Return the total number of errors.
	 */
	int getErrorCount();

	/**
	 * Get all errors, both global and field ones.
	 * @return a list of {@link ObjectError} instances
	 */
	List<ObjectError> getAllErrors();

	/**
	 * Are there any global errors?
	 * @return {@code true} if there are any global errors
	 * @see #hasFieldErrors()
	 */
	boolean hasGlobalErrors();

	/**
	 * Return the number of global errors.
	 * @return the number of global errors
	 * @see #getFieldErrorCount()
	 */
	int getGlobalErrorCount();

	/**
	 * Get all global errors.
	 * @return a list of {@link ObjectError} instances
	 */
	List<ObjectError> getGlobalErrors();

	/**
	 * Get the <i>first</i> global error, if any.
	 * @return the global error, or {@code null}
	 */
	@Nullable
	ObjectError getGlobalError();

	/**
	 * Are there any field errors?
	 * @return {@code true} if there are any errors associated with a field
	 * @see #hasGlobalErrors()
	 */
	boolean hasFieldErrors();

	/**
	 * Return the number of errors associated with a field.
	 * @return the number of errors associated with a field
	 * @see #getGlobalErrorCount()
	 */
	int getFieldErrorCount();

	/**
	 * Get all errors associated with a field.
	 * @return a List of {@link FieldError} instances
	 */
	List<FieldError> getFieldErrors();

	/**
	 * Get the <i>first</i> error associated with a field, if any.
	 * @return the field-specific error, or {@code null}
	 */
	@Nullable
	FieldError getFieldError();

	/**
	 * Are there any errors associated with the given field?
	 * @param field the field name
	 * @return {@code true} if there were any errors associated with the given field
	 */
	boolean hasFieldErrors(String field);

	/**
	 * Return the number of errors associated with the given field.
	 * @param field the field name
	 * @return the number of errors associated with the given field
	 */
	int getFieldErrorCount(String field);

	/**
	 * Get all errors associated with the given field.
	 * <p>Implementations should support not only full field names like
	 * "name" but also pattern matches like "na*" or "address.*".
	 * @param field the field name
	 * @return a List of {@link FieldError} instances
	 */
	List<FieldError> getFieldErrors(String field);

	/**
	 * Get the first error associated with the given field, if any.
	 * @param field the field name
	 * @return the field-specific error, or {@code null}
	 */
	@Nullable
	FieldError getFieldError(String field);

	/**
	 * Return the current value of the given field, either the current
	 * bean property value or a rejected update from the last binding.
	 * <p>Allows for convenient access to user-specified field values,
	 * even if there were type mismatches.
	 * @param field the field name
	 * @return the current value of the given field
	 */
	@Nullable
	Object getFieldValue(String field);

	/**
	 * Return the type of a given field.
	 * <p>Implementations should be able to determine the type even
	 * when the field value is {@code null}, for example from some
	 * associated descriptor.
	 * @param field the field name
	 * @return the type of the field, or {@code null} if not determinable
	 */
	@Nullable
	Class<?> getFieldType(String field);

}
