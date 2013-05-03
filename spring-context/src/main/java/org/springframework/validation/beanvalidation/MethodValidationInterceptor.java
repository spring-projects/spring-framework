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

import java.lang.reflect.Method;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.hibernate.validator.HibernateValidator;
import org.hibernate.validator.method.MethodConstraintViolation;
import org.hibernate.validator.method.MethodConstraintViolationException;
import org.hibernate.validator.method.MethodValidator;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.annotation.Validated;

/**
 * An AOP Alliance {@link MethodInterceptor} implementation that delegates to a
 * JSR-303 provider for performing method-level validation on annotated methods.
 *
 * <p>Applicable methods have JSR-303 constraint annotations on their parameters
 * and/or on their return value (in the latter case specified at the method level,
 * typically as inline annotation).
 *
 * <p>E.g.: {@code public @NotNull Object myValidMethod(@NotNull String arg1, @Max(10) int arg2)}
 *
 * <p>Validation groups can be specified through Spring's {@link Validated} annotation
 * at the type level of the containing target class, applying to all public service methods
 * of that class. By default, JSR-303 will validate against its default group only.
 *
 * <p>As of Spring 4.0, this functionality requires either a Bean Validation 1.1 provider
 * (such as Hibernate Validator 5.0) or the Bean Validation 1.0 API with Hibernate Validator
 * 4.2 or 4.3. The actual provider will be autodetected and automatically adapted.
 *
 * @author Juergen Hoeller
 * @since 3.1
 * @see MethodValidationPostProcessor
 * @see javax.validation.executable.ExecutableValidator
 * @see org.hibernate.validator.method.MethodValidator
 */
public class MethodValidationInterceptor implements MethodInterceptor {

	private static Method forExecutablesMethod;

	private static Method validateParametersMethod;

	private static Method validateReturnValueMethod;

	static {
		try {
			forExecutablesMethod = Validator.class.getMethod("forExecutables");
			Class<?> executableValidatorClass = forExecutablesMethod.getReturnType();
			validateParametersMethod = executableValidatorClass.getMethod(
					"validateParameters", Object.class, Method.class, Object[].class, Class[].class);
			validateReturnValueMethod = executableValidatorClass.getMethod(
					"validateReturnValue", Object.class, Method.class, Object.class, Class[].class);
		}
		catch (Exception ex) {
			// Bean Validation 1.1 ExecutableValidator API not available
		}
	}


	private final Validator validator;


	/**
	 * Create a new MethodValidationInterceptor using a default JSR-303 validator underneath.
	 */
	public MethodValidationInterceptor() {
		this(forExecutablesMethod != null ? Validation.buildDefaultValidatorFactory() :
				HibernateValidatorDelegate.buildValidatorFactory());
	}

	/**
	 * Create a new MethodValidationInterceptor using the given JSR-303 ValidatorFactory.
	 * @param validatorFactory the JSR-303 ValidatorFactory to use
	 */
	public MethodValidationInterceptor(ValidatorFactory validatorFactory) {
		this(validatorFactory.getValidator());
	}

	/**
	 * Create a new MethodValidationInterceptor using the given JSR-303 Validator.
	 * @param validator the JSR-303 Validator to use
	 */
	public MethodValidationInterceptor(Validator validator) {
		this.validator = validator;
	}


	@SuppressWarnings("unchecked")
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Class[] groups = determineValidationGroups(invocation);
		if (forExecutablesMethod != null) {
			Object executableValidator = ReflectionUtils.invokeMethod(forExecutablesMethod, this.validator);
			Set<ConstraintViolation<?>> result = (Set<ConstraintViolation<?>>)
					ReflectionUtils.invokeMethod(validateParametersMethod, executableValidator,
							invocation.getThis(), invocation.getMethod(), invocation.getArguments(), groups);
			if (!result.isEmpty()) {
				throw new ConstraintViolationException(result);
			}
			Object returnValue = invocation.proceed();
			result = (Set<ConstraintViolation<?>>)
					ReflectionUtils.invokeMethod(validateReturnValueMethod, executableValidator,
							invocation.getThis(), invocation.getMethod(), returnValue, groups);
			if (!result.isEmpty()) {
				throw new ConstraintViolationException(result);
			}
			return returnValue;
		}
		else {
			return HibernateValidatorDelegate.invokeWithinValidation(invocation, this.validator, groups);
		}
	}

	/**
	 * Determine the validation groups to validate against for the given method invocation.
	 * <p>Default are the validation groups as specified in the {@link Validated} annotation
	 * on the containing target class of the method.
	 * @param invocation the current MethodInvocation
	 * @return the applicable validation groups as a Class array
	 */
	protected Class[] determineValidationGroups(MethodInvocation invocation) {
		Validated valid = AnnotationUtils.findAnnotation(invocation.getThis().getClass(), Validated.class);
		return (valid != null ? valid.value() : new Class[0]);
	}


	/**
	 * Inner class to avoid a hard-coded Hibernate Validator 4.2/4.3 dependency.
	 */
	private static class HibernateValidatorDelegate {

		public static ValidatorFactory buildValidatorFactory() {
			return Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory();
		}

		public static Object invokeWithinValidation(MethodInvocation invocation, Validator validator, Class[] groups)
				throws Throwable {

			MethodValidator methodValidator = validator.unwrap(MethodValidator.class);
			Set<MethodConstraintViolation<Object>> result = methodValidator.validateAllParameters(
					invocation.getThis(), invocation.getMethod(), invocation.getArguments(), groups);
			if (!result.isEmpty()) {
				throw new MethodConstraintViolationException(result);
			}
			Object returnValue = invocation.proceed();
			result = methodValidator.validateReturnValue(
					invocation.getThis(), invocation.getMethod(), returnValue, groups);
			if (!result.isEmpty()) {
				throw new MethodConstraintViolationException(result);
			}
			return returnValue;
		}
	}

}
