/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.bind.support;

import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.validation.Errors;
import org.springframework.validation.SmartValidator;
import org.springframework.validation.Validator;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.testfixture.method.ResolvableMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultDataBinderFactory}.
 */
class DefaultDataBinderFactoryTests {

	@Test
	void jakartaValidatorExcludedWhenMethodValidationApplicable() throws Exception {
		DefaultDataBinderFactory binderFactory = new DefaultDataBinderFactory(null);
		binderFactory.setMethodValidationApplicable(true);

		MethodParameter parameter = ResolvableMethod.on(DefaultDataBinderFactoryTests.class)
				.named("handle").build().annotPresent(Valid.class).arg();

		WebDataBinder dataBinder = binderFactory.createBinder(
				new ServletWebRequest(new MockHttpServletRequest()), new Foo(), "foo",
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
