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

package org.springframework.web.method.support;

import java.lang.reflect.Method;

import jakarta.validation.Validator;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.lang.Nullable;
import org.springframework.validation.BindingResult;
import org.springframework.validation.MessageCodesResolver;
import org.springframework.validation.beanvalidation.DefaultMethodValidator;
import org.springframework.validation.beanvalidation.MethodValidationAdapter;
import org.springframework.validation.beanvalidation.MethodValidationResult;
import org.springframework.validation.beanvalidation.MethodValidator;
import org.springframework.validation.beanvalidation.ParameterErrors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.annotation.ModelFactory;

/**
 * {@link org.springframework.validation.beanvalidation.MethodValidator} for
 * use with {@code @RequestMapping} methods. Helps to determine object names
 * and populates {@link BindingResult} method arguments with errors from
 * {@link MethodValidationResult#getBeanResults() beanResults}.
 *
 * @author Rossen Stoyanchev
 * @since 6.1
 */
public class HandlerMethodValidator extends DefaultMethodValidator {


	public HandlerMethodValidator(MethodValidationAdapter adapter) {
		super(adapter);
		adapter.setBindingResultNameResolver(this::determineObjectName);
	}

	private String determineObjectName(MethodParameter param, @Nullable Object argument) {
		if (param.hasParameterAnnotation(RequestBody.class) || param.hasParameterAnnotation(RequestPart.class)) {
			return Conventions.getVariableNameForParameter(param);
		}
		else {
			return ((param.getParameterIndex() != -1) ?
					ModelFactory.getNameForParameter(param) :
					ModelFactory.getNameForReturnValue(argument, param));
		}
	}


	@Override
	protected void handleArgumentsResult(
			Object bean, Method method, Object[] arguments, Class<?>[] groups, MethodValidationResult result) {

		if (result.getConstraintViolations().isEmpty()) {
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
		result.throwIfViolationsPresent();
	}


	/**
	 * Create a {@link MethodValidator} if Bean Validation is enabled in Spring MVC or WebFlux.
	 * @param bindingInitializer for the configured Validator and MessageCodesResolver
	 * @param parameterNameDiscoverer the {@code ParameterNameDiscoverer} to use
	 * for {@link MethodValidationAdapter#setParameterNameDiscoverer}
	 */
	@Nullable
	public static MethodValidator from(
			@Nullable WebBindingInitializer bindingInitializer,
			@Nullable ParameterNameDiscoverer parameterNameDiscoverer) {

		if (bindingInitializer instanceof ConfigurableWebBindingInitializer configurableInitializer) {
			if (configurableInitializer.getValidator() instanceof Validator validator) {
				MethodValidationAdapter validationAdapter = new MethodValidationAdapter(validator);
				if (parameterNameDiscoverer != null) {
					validationAdapter.setParameterNameDiscoverer(parameterNameDiscoverer);
				}
				MessageCodesResolver codesResolver = configurableInitializer.getMessageCodesResolver();
				if (codesResolver != null) {
					validationAdapter.setMessageCodesResolver(codesResolver);
				}
				return new HandlerMethodValidator(validationAdapter);
			}
		}
		return null;
	}

}
