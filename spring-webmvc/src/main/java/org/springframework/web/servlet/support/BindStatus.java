/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.servlet.support;

import java.beans.PropertyEditor;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.context.NoSuchMessageException;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ObjectError;
import org.springframework.web.util.HtmlUtils;

/**
 * Simple adapter to expose the bind status of a field or object.
 * Set as a variable both by the JSP bind tag and FreeMarker macros.
 *
 * <p>Obviously, object status representations (i.e. errors at the object level
 * rather than the field level) do not have an expression and a value but only
 * error codes and messages. For simplicity's sake and to be able to use the same
 * tags and macros, the same status class is used for both scenarios.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Darren Davison
 * @see RequestContext#getBindStatus
 * @see org.springframework.web.servlet.tags.BindTag
 * @see org.springframework.web.servlet.view.AbstractTemplateView#setExposeSpringMacroHelpers
 */
public class BindStatus {

	private final RequestContext requestContext;

	private final String path;

	private final boolean htmlEscape;

	private final @Nullable String expression;

	private final @Nullable Errors errors;

	private final @Nullable String[] errorCodes;

	private String @Nullable [] errorMessages;

	private @Nullable List<? extends ObjectError> objectErrors;

	private @Nullable Object value;

	private @Nullable Class<?> valueType;

	private @Nullable Object actualValue;

	private @Nullable PropertyEditor editor;

	private @Nullable BindingResult bindingResult;


	/**
	 * Create a new BindStatus instance, representing a field or object status.
	 * @param requestContext the current RequestContext
	 * @param path the bean and property path for which values and errors
	 * will be resolved (for example, "customer.address.street")
	 * @param htmlEscape whether to HTML-escape error messages and string values
	 * @throws IllegalStateException if no corresponding Errors object found
	 */
	public BindStatus(RequestContext requestContext, String path, boolean htmlEscape) throws IllegalStateException {
		this.requestContext = requestContext;
		this.path = path;
		this.htmlEscape = htmlEscape;

		// determine name of the object and property
		String beanName;
		int dotPos = path.indexOf('.');
		if (dotPos == -1) {
			// property not set, only the object itself
			beanName = path;
			this.expression = null;
		}
		else {
			beanName = path.substring(0, dotPos);
			this.expression = path.substring(dotPos + 1);
		}

		this.errors = requestContext.getErrors(beanName, false);

		if (this.errors != null) {
			// Usual case: A BindingResult is available as request attribute.
			// Can determine error codes and messages for the given expression.
			// Can use a custom PropertyEditor, as registered by a form controller.
			if (this.expression != null) {
				if ("*".equals(this.expression)) {
					this.objectErrors = this.errors.getAllErrors();
				}
				else if (this.expression.endsWith("*")) {
					this.objectErrors = this.errors.getFieldErrors(this.expression);
				}
				else {
					this.objectErrors = this.errors.getFieldErrors(this.expression);
					this.value = this.errors.getFieldValue(this.expression);
					this.valueType = this.errors.getFieldType(this.expression);
					if (this.errors instanceof BindingResult br) {
						this.bindingResult = br;
						this.actualValue = this.bindingResult.getRawFieldValue(this.expression);
						this.editor = this.bindingResult.findEditor(this.expression, null);
					}
					else {
						this.actualValue = this.value;
					}
				}
			}
			else {
				this.objectErrors = this.errors.getGlobalErrors();
			}
			this.errorCodes = initErrorCodes(this.objectErrors);
		}

		else {
			// No BindingResult available as request attribute:
			// Probably forwarded directly to a form view.
			// Let's do the best we can: extract a plain target if appropriate.
			Object target = requestContext.getModelObject(beanName);
			if (target == null) {
				throw new IllegalStateException("Neither BindingResult nor plain target object for bean name '" +
						beanName + "' available as request attribute");
			}
			if (this.expression != null && !"*".equals(this.expression) && !this.expression.endsWith("*")) {
				BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(target);
				this.value = bw.getPropertyValue(this.expression);
				this.valueType = bw.getPropertyType(this.expression);
				this.actualValue = this.value;
			}
			this.errorCodes = new String[0];
			this.errorMessages = new String[0];
		}

		if (htmlEscape && this.value instanceof String text) {
			this.value = HtmlUtils.htmlEscape(text);
		}
	}

	/**
	 * Extract the error codes from the ObjectError list.
	 */
	private static @Nullable String[] initErrorCodes(List<? extends ObjectError> objectErrors) {
		@Nullable String[] errorCodes = new String[objectErrors.size()];
		for (int i = 0; i < objectErrors.size(); i++) {
			ObjectError error = objectErrors.get(i);
			errorCodes[i] = error.getCode();
		}
		return errorCodes;
	}


	/**
	 * Return the bean and property path for which values and errors
	 * will be resolved (for example, "customer.address.street").
	 */
	public String getPath() {
		return this.path;
	}

	/**
	 * Return a bind expression that can be used in HTML forms as input name
	 * for the respective field, or {@code null} if not field-specific.
	 * <p>Returns a bind path appropriate for resubmission, for example, "address.street".
	 * Note that the complete bind path as required by the bind tag is
	 * "customer.address.street", if bound to a "customer" bean.
	 */
	public @Nullable String getExpression() {
		return this.expression;
	}

	/**
	 * Return the current value of the field, i.e. either the property value
	 * or a rejected update, or {@code null} if not field-specific.
	 * <p>This value will be an HTML-escaped String if the original value
	 * already was a String.
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

	/**
	 * Get the '{@code Class}' type of the field. Favor this instead of
	 * '{@code getValue().getClass()}' since '{@code getValue()}' may
	 * return '{@code null}'.
	 */
	public @Nullable Class<?> getValueType() {
		return this.valueType;
	}

	/**
	 * Return the actual value of the field, i.e. the raw property value,
	 * or {@code null} if not available.
	 */
	public @Nullable Object getActualValue() {
		return this.actualValue;
	}

	/**
	 * Return a suitable display value for the field, i.e. the stringified
	 * value if not null, and an empty string in case of a null value.
	 * <p>This value will be an HTML-escaped String if the original value
	 * was non-null: the {@code toString} result of the original value
	 * will get HTML-escaped.
	 */
	public String getDisplayValue() {
		if (this.value instanceof String displayValue) {
			return displayValue;
		}
		if (this.value != null) {
			return (this.htmlEscape ? HtmlUtils.htmlEscape(this.value.toString()) : this.value.toString());
		}
		return "";
	}

	/**
	 * Return if this status represents a field or object error.
	 */
	public boolean isError() {
		return (this.errorCodes.length > 0);
	}

	/**
	 * Return the error codes for the field or object, if any.
	 * Returns an empty array instead of null if none.
	 */
	public @Nullable String[] getErrorCodes() {
		return this.errorCodes;
	}

	/**
	 * Return the first error codes for the field or object, if any.
	 */
	public String getErrorCode() {
		return (this.errorCodes.length > 0 ? Objects.requireNonNull(this.errorCodes[0],
				"Error code must not be null") : "");
	}

	/**
	 * Return the resolved error messages for the field or object,
	 * if any. Returns an empty array instead of null if none.
	 */
	public String[] getErrorMessages() {
		return initErrorMessages();
	}

	/**
	 * Return the first error message for the field or object, if any.
	 */
	public String getErrorMessage() {
		String[] errorMessages = initErrorMessages();
		return (errorMessages.length > 0 ? errorMessages[0] : "");
	}

	/**
	 * Return an error message string, concatenating all messages
	 * separated by the given delimiter.
	 * @param delimiter separator string, for example, ", " or "<br>"
	 * @return the error message string
	 */
	public String getErrorMessagesAsString(String delimiter) {
		return StringUtils.arrayToDelimitedString(initErrorMessages(), delimiter);
	}

	/**
	 * Extract the error messages from the ObjectError list.
	 */
	private String[] initErrorMessages() throws NoSuchMessageException {
		if (this.errorMessages == null) {
			if (this.objectErrors != null) {
				this.errorMessages = new String[this.objectErrors.size()];
				for (int i = 0; i < this.objectErrors.size(); i++) {
					ObjectError error = this.objectErrors.get(i);
					this.errorMessages[i] = this.requestContext.getMessage(error, this.htmlEscape);
				}
			}
			else {
				this.errorMessages = new String[0];
			}
		}
		return this.errorMessages;
	}

	/**
	 * Return the Errors instance (typically a BindingResult) that this
	 * bind status is currently associated with.
	 * @return the current Errors instance, or {@code null} if none
	 * @see org.springframework.validation.BindingResult
	 */
	public @Nullable Errors getErrors() {
		return this.errors;
	}

	/**
	 * Return the PropertyEditor for the property that this bind status
	 * is currently bound to.
	 * @return the current PropertyEditor, or {@code null} if none
	 */
	public @Nullable PropertyEditor getEditor() {
		return this.editor;
	}

	/**
	 * Find a PropertyEditor for the given value class, associated with
	 * the property that this bound status is currently bound to.
	 * @param valueClass the value class that an editor is needed for
	 * @return the associated PropertyEditor, or {@code null} if none
	 */
	public @Nullable PropertyEditor findEditor(Class<?> valueClass) {
		return (this.bindingResult != null ? this.bindingResult.findEditor(this.expression, valueClass) : null);
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("BindStatus: ");
		sb.append("expression=[").append(this.expression).append("]; ");
		sb.append("value=[").append(this.value).append(']');
		if (!ObjectUtils.isEmpty(this.errorCodes)) {
			sb.append("; errorCodes=").append(Arrays.asList(this.errorCodes));
		}
		return sb.toString();
	}

}
