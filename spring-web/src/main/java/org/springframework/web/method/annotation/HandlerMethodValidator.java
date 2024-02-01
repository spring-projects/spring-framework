/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.function.Predicate;

import jakarta.validation.Validator;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.beanvalidation.MethodValidationAdapter;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.MethodValidator;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebBindingInitializer;

/**
 * {@link MethodValidator} that
 * uses Bean Validation to validate {@code @RequestMapping} method arguments.
 *
 * <p>Handles validation results by populating {@link BindingResult} method
 * arguments with errors from {@link MethodValidationResult#getBeanResults()
 * beanResults}. Also, helps to determine parameter names for
 * {@code @ModelAttribute} and {@code @RequestBody} parameters.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public final class HandlerMethodValidator implements MethodValidator {

	private static final MethodValidationAdapter.ObjectNameResolver objectNameResolver = new WebObjectNameResolver();


	private final MethodValidationAdapter validationAdapter;

	private final Predicate<MethodParameter> modelAttributePredicate;

	private final Predicate<MethodParameter> requestParamPredicate;


	private HandlerMethodValidator(MethodValidationAdapter validationAdapter,
			Predicate<MethodParameter> modelAttributePredicate, Predicate<MethodParameter> requestParamPredicate) {

		this.validationAdapter = validationAdapter;
		this.modelAttributePredicate = modelAttributePredicate;
		this.requestParamPredicate = requestParamPredicate;
	}


	@Override
	public Class<?>[] determineValidationGroups(Object target, Method method) {
		return this.validationAdapter.determineValidationGroups(target, method);
	}

	@Override
	public void applyArgumentValidation(
			Object target, Method method, @Nullable MethodParameter[] parameters,
			Object[] arguments, Class<?>[] groups) {

		MethodValidationResult result = validateArguments(target, method, parameters, arguments, groups);
		if (!result.hasErrors()) {
			return;
		}

		if (!result.getBeanResults().isEmpty()) {
			int bindingResultCount = 0;
			for (ParameterErrors errors : result.getBeanResults()) {
				for (Object arg : arguments) {
					if (arg instanceof BindingResult bindingResult) {
						if (bindingResult.getObjectName().equals(errors.getObjectName())) {
							bindingResult.addAllErrors(errors);
							bindingResultCount++;
							break;
						}
					}
				}
			}
			if (result.getAllValidationResults().size() == bindingResultCount) {
				return;
			}
		}

		throw new HandlerMethodValidationException(
				result, this.modelAttributePredicate, this.requestParamPredicate);
	}

	@Override
	public MethodValidationResult validateArguments(
			Object target, Method method, @Nullable MethodParameter[] parameters,
			Object[] arguments, Class<?>[] groups) {

		return this.validationAdapter.validateArguments(target, method, parameters, arguments, groups);
	}

	@Override
	public void applyReturnValueValidation(
			Object target, Method method, @Nullable MethodParameter returnType,
			@Nullable Object returnValue, Class<?>[] groups) {

		MethodValidationResult result = validateReturnValue(target, method, returnType, returnValue, groups);
		if (result.hasErrors()) {
			throw new HandlerMethodValidationException(result);
		}
	}

	@Override
	public MethodValidationResult validateReturnValue(Object target, Method method,
			@Nullable MethodParameter returnType, @Nullable Object returnValue, Class<?>[] groups) {

		return this.validationAdapter.validateReturnValue(target, method, returnType, returnValue, groups);
	}


	/**
	 * Static factory method to create a {@link HandlerMethodValidator} when Bean
	 * Validation is enabled for use via {@link ConfigurableWebBindingInitializer},
	 * for example in Spring MVC or WebFlux config.
	 */
	@Nullable
	public static MethodValidator from(
			@Nullable WebBindingInitializer initializer, @Nullable ParameterNameDiscoverer paramNameDiscoverer,
			Predicate<MethodParameter> modelAttributePredicate, Predicate<MethodParameter> requestParamPredicate) {

		if (initializer instanceof ConfigurableWebBindingInitializer configurableInitializer) {
			Validator validator = getValidator(configurableInitializer);
			if (validator != null) {
				MethodValidationAdapter adapter = new MethodValidationAdapter(validator);
				adapter.setObjectNameResolver(objectNameResolver);
				if (paramNameDiscoverer != null) {
					adapter.setParameterNameDiscoverer(paramNameDiscoverer);
				}
				MessageCodesResolver codesResolver = configurableInitializer.getMessageCodesResolver();
				if (codesResolver != null) {
					adapter.setMessageCodesResolver(codesResolver);
				}
				return new HandlerMethodValidator(adapter, modelAttributePredicate, requestParamPredicate);
			}
		}
		return null;
	}

	@Nullable
	private static Validator getValidator(ConfigurableWebBindingInitializer initializer) {
		if (initializer.getValidator() instanceof Validator validator) {
			return validator;
		}
		if (initializer.getValidator() instanceof SmartValidator smartValidator) {
			return smartValidator.unwrap(Validator.class);
		}
		return null;
	}


	/**
	 * ObjectNameResolver for web controller methods.
	 */
	private static class WebObjectNameResolver implements MethodValidationAdapter.ObjectNameResolver {

		@Override
		public String resolveName(MethodParameter param, @Nullable Object value) {
			if (param.hasParameterAnnotation(RequestBody.class) || param.hasParameterAnnotation(RequestPart.class)) {
				return Conventions.getVariableNameForParameter(param);
			}
			else {
				return (param.getParameterIndex() != -1 ?
						ModelFactory.getNameForParameter(param) :
						ModelFactory.getNameForReturnValue(value, param));
			}
		}
	}

}
