/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.validation.beanvalidation;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.validation.ConstraintViolation;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;

import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.SmartValidator;

/**
 * Adapter that takes a JSR-303 {@code javax.validator.Validator}
 * and exposes it as a Spring {@link org.springframework.validation.Validator}
 * while also exposing the original JSR-303 Validator interface itself.
 *
 * <p>Can be used as a programmatic wrapper. Also serves as base class for
 * {@link CustomValidatorBean} and {@link LocalValidatorFactoryBean}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SpringValidatorAdapter implements SmartValidator, javax.validation.Validator {

	private static final Set<String> internalAnnotationAttributes = new HashSet<String>(3);

	static {
		internalAnnotationAttributes.add("message");
		internalAnnotationAttributes.add("groups");
		internalAnnotationAttributes.add("payload");
	}

	private javax.validation.Validator targetValidator;


	/**
	 * Create a new SpringValidatorAdapter for the given JSR-303 Validator.
	 * @param targetValidator the JSR-303 Validator to wrap
	 */
	public SpringValidatorAdapter(javax.validation.Validator targetValidator) {
		Assert.notNull(targetValidator, "Target Validator must not be null");
		this.targetValidator = targetValidator;
	}

	SpringValidatorAdapter() {
	}

	void setTargetValidator(javax.validation.Validator targetValidator) {
		this.targetValidator = targetValidator;
	}


	//---------------------------------------------------------------------
	// Implementation of Spring Validator interface
	//---------------------------------------------------------------------

	public boolean supports(Class<?> clazz) {
		return true;
	}

	public void validate(Object target, Errors errors) {
		processConstraintViolations(this.targetValidator.validate(target), errors);
	}

	@SuppressWarnings("rawtypes")
	public void validate(Object target, Errors errors, Object... validationHints) {
		Set<Class> groups = new LinkedHashSet<Class>();
		if (validationHints != null) {
			for (Object hint : validationHints) {
				if (hint instanceof Class) {
					groups.add((Class) hint);
				}
			}
		}
		processConstraintViolations(
				this.targetValidator.validate(target, groups.toArray(new Class[groups.size()])), errors);
	}

	/**
	 * Process the given JSR-303 ConstraintViolations, adding corresponding errors to
	 * the provided Spring {@link Errors} object.
	 * @param violations the JSR-303 ConstraintViolation results
	 * @param errors the Spring errors object to register to
	 */
	protected void processConstraintViolations(Set<ConstraintViolation<Object>> violations, Errors errors) {
		for (ConstraintViolation<Object> violation : violations) {
			String field = violation.getPropertyPath().toString();
			FieldError fieldError = errors.getFieldError(field);
			if (fieldError == null || !fieldError.isBindingFailure()) {
				try {
					ConstraintDescriptor<?> cd = violation.getConstraintDescriptor();
					String errorCode = cd.getAnnotation().annotationType().getSimpleName();
					Object[] errorArgs = getArgumentsForConstraint(errors.getObjectName(), field, cd);
					if (errors instanceof BindingResult) {
						// Can do custom FieldError registration with invalid value from ConstraintViolation,
						// as necessary for Hibernate Validator compatibility (non-indexed set path in field)
						BindingResult bindingResult = (BindingResult) errors;
						String nestedField = bindingResult.getNestedPath() + field;
						if ("".equals(nestedField)) {
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode);
							bindingResult.addError(new ObjectError(
									errors.getObjectName(), errorCodes, errorArgs, violation.getMessage()));
						}
						else {
							Object invalidValue = violation.getInvalidValue();
							if (!"".equals(field) && (invalidValue == violation.getLeafBean() ||
									(field.contains(".") && !field.contains("[]")))) {
								// Possibly a bean constraint with property path: retrieve the actual property value.
								// However, explicitly avoid this for "address[]" style paths that we can't handle.
								invalidValue = bindingResult.getRawFieldValue(field);
							}
							String[] errorCodes = bindingResult.resolveMessageCodes(errorCode, field);
							bindingResult.addError(new FieldError(
									errors.getObjectName(), nestedField, invalidValue, false,
									errorCodes, errorArgs, violation.getMessage()));
						}
					}
					else {
						// got no BindingResult - can only do standard rejectValue call
						// with automatic extraction of the current field value
						errors.rejectValue(field, errorCode, errorArgs, violation.getMessage());
					}
				}
				catch (NotReadablePropertyException ex) {
					throw new IllegalStateException("JSR-303 validated property '" + field +
							"' does not have a corresponding accessor for Spring data binding - " +
							"check your DataBinder's configuration (bean property versus direct field access)", ex);
				}
			}
		}
	}

	/**
	 * Return FieldError arguments for a validation error on the given field.
	 * Invoked for each violated constraint.
	 * <p>The default implementation returns a first argument indicating the field name
	 * (of type DefaultMessageSourceResolvable, with "objectName.field" and "field" as codes).
	 * Afterwards, it adds all actual constraint annotation attributes (i.e. excluding
	 * "message", "groups" and "payload") in alphabetical order of their attribute names.
	 * <p>Can be overridden to e.g. add further attributes from the constraint descriptor.
	 * @param objectName the name of the target object
	 * @param field the field that caused the binding error
	 * @param descriptor the JSR-303 constraint descriptor
	 * @return the Object array that represents the FieldError arguments
	 * @see org.springframework.validation.FieldError#getArguments
	 * @see org.springframework.context.support.DefaultMessageSourceResolvable
	 * @see org.springframework.validation.DefaultBindingErrorProcessor#getArgumentsForBindError
	 */
	protected Object[] getArgumentsForConstraint(String objectName, String field, ConstraintDescriptor<?> descriptor) {
		List<Object> arguments = new LinkedList<Object>();
		String[] codes = new String[] {objectName + Errors.NESTED_PATH_SEPARATOR + field, field};
		arguments.add(new DefaultMessageSourceResolvable(codes, field));
		// Using a TreeMap for alphabetical ordering of attribute names
		Map<String, Object> attributesToExpose = new TreeMap<String, Object>();
		for (Map.Entry<String, Object> entry : descriptor.getAttributes().entrySet()) {
			String attributeName = entry.getKey();
			Object attributeValue = entry.getValue();
			if (!internalAnnotationAttributes.contains(attributeName)) {
				attributesToExpose.put(attributeName, attributeValue);
			}
		}
		arguments.addAll(attributesToExpose.values());
		return arguments.toArray(new Object[arguments.size()]);
	}


	//---------------------------------------------------------------------
	// Implementation of JSR-303 Validator interface
	//---------------------------------------------------------------------

	public <T> Set<ConstraintViolation<T>> validate(T object, Class<?>... groups) {
		return this.targetValidator.validate(object, groups);
	}

	public <T> Set<ConstraintViolation<T>> validateProperty(T object, String propertyName, Class<?>... groups) {
		return this.targetValidator.validateProperty(object, propertyName, groups);
	}

	public <T> Set<ConstraintViolation<T>> validateValue(
			Class<T> beanType, String propertyName, Object value, Class<?>... groups) {

		return this.targetValidator.validateValue(beanType, propertyName, value, groups);
	}

	public BeanDescriptor getConstraintsForClass(Class<?> clazz) {
		return this.targetValidator.getConstraintsForClass(clazz);
	}

	public <T> T unwrap(Class<T> type) {
		return this.targetValidator.unwrap(type);
	}

}
