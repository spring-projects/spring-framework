/*
 * Copyright 2002-2009 the original author or authors.
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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.metadata.BeanDescriptor;
import javax.validation.metadata.ConstraintDescriptor;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;
import org.springframework.validation.Validator;

/**
 * Adapter that takes a JSR-303 <code>javax.validator.Validator</code>
 * and exposes it as a Spring {@link org.springframework.validation.Validator}
 * while also exposing the original JSR-303 Validator interface itself.
 *
 * <p>Can be used as a programmatic wrapper. Also serves as base class for
 * {@link CustomValidatorBean} and {@link LocalValidatorFactoryBean}.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class SpringValidatorAdapter implements Validator, javax.validation.Validator {

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
		Set<ConstraintViolation<Object>> result = this.targetValidator.validate(target);
		for (ConstraintViolation<Object> violation : result) {
			String field = violation.getPropertyPath().toString();
			FieldError fieldError = errors.getFieldError(field);
			if (fieldError == null || !fieldError.isBindingFailure()) {
				errors.rejectValue(field,
						violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName(),
						getArgumentsForConstraint(errors.getObjectName(), field, violation.getConstraintDescriptor()),
						violation.getMessage());
			}
		}
	}

	/**
	 * Return FieldError arguments for a validation error on the given field.
	 * Invoked for each violated constraint.
	 * <p>The default implementation returns a single argument of type
	 * DefaultMessageSourceResolvable, with "objectName.field" and "field" as codes.
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
		arguments.addAll(descriptor.getAttributes().values());
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
