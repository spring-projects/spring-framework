/*
 * Copyright 2002-2017 the original author or authors.
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

import jakarta.validation.MessageInterpolator;
import jakarta.validation.TraversableResolver;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorContext;
import jakarta.validation.ValidatorFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

/**
 * Configurable bean class that exposes a specific JSR-303 Validator
 * through its original interface as well as through the Spring
 * {@link org.springframework.validation.Validator} interface.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class CustomValidatorBean extends SpringValidatorAdapter implements Validator, InitializingBean {

	@Nullable
	private ValidatorFactory validatorFactory;

	@Nullable
	private MessageInterpolator messageInterpolator;

	@Nullable
	private TraversableResolver traversableResolver;


	/**
	 * Set the ValidatorFactory to obtain the target Validator from.
	 * <p>Default is {@link jakarta.validation.Validation#buildDefaultValidatorFactory()}.
	 */
	public void setValidatorFactory(ValidatorFactory validatorFactory) {
		this.validatorFactory = validatorFactory;
	}

	/**
	 * Specify a custom MessageInterpolator to use for this Validator.
	 */
	public void setMessageInterpolator(MessageInterpolator messageInterpolator) {
		this.messageInterpolator = messageInterpolator;
	}

	/**
	 * Specify a custom TraversableResolver to use for this Validator.
	 */
	public void setTraversableResolver(TraversableResolver traversableResolver) {
		this.traversableResolver = traversableResolver;
	}


	@Override
	public void afterPropertiesSet() {
		if (this.validatorFactory == null) {
			this.validatorFactory = Validation.buildDefaultValidatorFactory();
		}

		ValidatorContext validatorContext = this.validatorFactory.usingContext();
		MessageInterpolator targetInterpolator = this.messageInterpolator;
		if (targetInterpolator == null) {
			targetInterpolator = this.validatorFactory.getMessageInterpolator();
		}
		validatorContext.messageInterpolator(new LocaleContextMessageInterpolator(targetInterpolator));
		if (this.traversableResolver != null) {
			validatorContext.traversableResolver(this.traversableResolver);
		}

		setTargetValidator(validatorContext.getValidator());
	}

}
