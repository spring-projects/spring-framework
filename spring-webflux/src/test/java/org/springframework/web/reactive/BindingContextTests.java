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

package org.springframework.web.reactive;

import java.lang.reflect.Method;

import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;

import org.springframework.core.ResolvableType;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link BindingContext}.
 */
class BindingContextTests {

	@Test
	void jakartaValidatorExcludedWhenMethodValidationApplicable() throws Exception {
		BindingContext bindingContext = new BindingContext(null);
		bindingContext.setMethodValidationApplicable(true);

		Method method = getClass().getDeclaredMethod("handleValidObject", Foo.class);
		ResolvableType targetType = ResolvableType.forMethodParameter(method, 0);

		WebDataBinder binder = bindingContext.createDataBinder(
				MockServerWebExchange.from(MockServerHttpRequest.get("")), new Foo(), "foo", targetType);

		Validator springValidator = mock(Validator.class);
		when(springValidator.supports(Foo.class)).thenReturn(true);
		binder.addValidators(springValidator);

		LocalValidatorFactoryBean beanValidator = new LocalValidatorFactoryBean();
		beanValidator.afterPropertiesSet();
		binder.addValidators(beanValidator);

		WrappedBeanValidator wrappedBeanValidator = new WrappedBeanValidator(beanValidator);
		binder.addValidators(wrappedBeanValidator);

		assertThat(binder.getValidatorsToApply()).containsExactly(springValidator);
	}

	@SuppressWarnings("unused")
	private void handleValidObject(@Valid Foo foo) {
	}


	private record WrappedBeanValidator(jakarta.validation.Validator validator) implements SmartValidator {

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


	private static final class Foo {}

}
