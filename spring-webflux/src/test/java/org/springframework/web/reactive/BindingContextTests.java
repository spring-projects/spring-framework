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

package org.springframework.web.reactive;

import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link BindingContext}.
 */
class BindingContextTests {

	@Test
	void jakartaValidatorExcludedWhenMethodValidationApplicable() {
		BindingContext bindingContext = new BindingContext(null);
		bindingContext.setMethodValidationApplicable(true);

		MethodParameter parameter = ResolvableMethod.on(BindingContextTests.class)
				.named("handle").build().annotPresent(Valid.class).arg();

		WebDataBinder dataBinder = bindingContext.createDataBinder(
				MockServerWebExchange.from(MockServerHttpRequest.get("")), new Foo(), "foo",
				ResolvableType.forMethodParameter(parameter));

		Validator springValidator = mock(Validator.class);
		when(springValidator.supports(Foo.class)).thenReturn(true);
		dataBinder.addValidators(springValidator);

		LocalValidatorFactoryBean beanValidator = new LocalValidatorFactoryBean();
		beanValidator.afterPropertiesSet();
		dataBinder.addValidators(beanValidator);

		WrappedBeanValidator wrappedBeanValidator = new WrappedBeanValidator(beanValidator);
		dataBinder.addValidators(wrappedBeanValidator);

		assertThat(dataBinder.getValidatorsToApply()).containsExactly(springValidator);
	}


	@SuppressWarnings("unused")
	private void handle(@Valid Foo foo) {
	}


	private static class WrappedBeanValidator implements SmartValidator {

		private final jakarta.validation.Validator validator;

		private WrappedBeanValidator(jakarta.validation.Validator validator) {
			this.validator = validator;
		}

		@Override
		public boolean supports(Class<?> clazz) {
			return true;
		}

		@Override
		public void validate(Object target, Errors errors, Object... validationHints) {
		}

		@Override
		public void validate(Object target, Errors errors) {
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T unwrap(Class<T> type) {
			return (T) this.validator;
		}
	}


	private static class Foo {}

}
