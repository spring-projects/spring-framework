/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
