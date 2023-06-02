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

package org.springframework.validation.beanvalidation;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.function.Supplier;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.function.SingletonSupplier;
import org.springframework.validation.annotation.Validated;

/**
 * Helper class to apply method-level validation on annotated methods via
 * {@link jakarta.validation.Valid}.
 *
 * <p>Used by {@link MethodValidationInterceptor}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1.0
 */
public class MethodValidationDelegate {

	private final Supplier<Validator> validator;


	/**
	 * Create an instance using a default JSR-303 validator underneath.
	 */
	public MethodValidationDelegate() {
		this.validator = SingletonSupplier.of(() -> Validation.buildDefaultValidatorFactory().getValidator());
	}

	/**
	 * Create an instance using the given JSR-303 ValidatorFactory.
	 * @param validatorFactory the JSR-303 ValidatorFactory to use
	 */
	public MethodValidationDelegate(ValidatorFactory validatorFactory) {
		this.validator = SingletonSupplier.of(validatorFactory::getValidator);
	}

	/**
	 * Create an instance using the given JSR-303 Validator.
	 * @param validator the JSR-303 Validator to use
	 */
	public MethodValidationDelegate(Validator validator) {
		this.validator = () -> validator;
	}

	/**
	 * Create an instance for the supplied (potentially lazily initialized) Validator.
	 * @param validator a Supplier for the Validator to use
	 */
	public MethodValidationDelegate(Supplier<Validator> validator) {
		this.validator = validator;
	}


	/**
	 * Use this method determine the validation groups to pass into
	 * {@link #validateMethodArguments(Object, Method, Object[], Class[])} and
	 * {@link #validateMethodReturnValue(Object, Method, Object, Class[])}.
	 * <p>Default are the validation groups as specified in the {@link Validated}
	 * annotation on the method, or on the containing target class of the method,
	 * or for an AOP proxy without a target (with all behavior in advisors), also
	 * check on proxied interfaces.
	 * @param target the target Object
	 * @param method the target method
	 * @return the applicable validation groups as a {@code Class} array
	 */
	public Class<?>[] determineValidationGroups(Object target, Method method) {
		Validated validatedAnn = AnnotationUtils.findAnnotation(method, Validated.class);
		if (validatedAnn == null) {
			if (AopUtils.isAopProxy(target)) {
				for (Class<?> type : AopProxyUtils.proxiedUserInterfaces(target)) {
					validatedAnn = AnnotationUtils.findAnnotation(type, Validated.class);
					if (validatedAnn != null) {
						break;
					}
				}
			}
			else {
				validatedAnn = AnnotationUtils.findAnnotation(target.getClass(), Validated.class);
			}
		}
		return (validatedAnn != null ? validatedAnn.value() : new Class<?>[0]);
	}

	/**
	 * Validate the given method arguments and raise {@link ConstraintViolation}
	 * in case of any errors.
	 * @param target the target Object
	 * @param method the target method
	 * @param arguments candidate arguments for a method invocation
	 * @param groups groups for validation determined via
	 * {@link #determineValidationGroups(Object, Method)}
	 */
	public void validateMethodArguments(Object target, Method method, Object[] arguments, Class<?>[] groups) {
		ExecutableValidator execVal = this.validator.get().forExecutables();
		Set<ConstraintViolation<Object>> result;
		try {
			result = execVal.validateParameters(target, method, arguments, groups);
		}
		catch (IllegalArgumentException ex) {
			// Probably a generic type mismatch between interface and impl as reported in SPR-12237 / HV-1011
			// Let's try to find the bridged method on the implementation class...
			Method mostSpecificMethod = ClassUtils.getMostSpecificMethod(method, target.getClass());
			Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(mostSpecificMethod);
			result = execVal.validateParameters(target, bridgedMethod, arguments, groups);
		}
		if (!result.isEmpty()) {
			throw new ConstraintViolationException(result);
		}
	}

	/**
	 * Validate the given return value and raise {@link ConstraintViolation}
	 * in case of any errors.
	 * @param target the target Object
	 * @param method the target method
	 * @param returnValue value returned from invoking the target method
	 * @param groups groups for validation determined via
	 * {@link #determineValidationGroups(Object, Method)}
	 */
	public void validateMethodReturnValue(
			Object target, Method method, @Nullable Object returnValue, Class<?>[] groups) {

		ExecutableValidator execVal = this.validator.get().forExecutables();
		Set<ConstraintViolation<Object>> result = execVal.validateReturnValue(target, method, returnValue, groups);
		if (!result.isEmpty()) {
			throw new ConstraintViolationException(result);
		}
	}

}
