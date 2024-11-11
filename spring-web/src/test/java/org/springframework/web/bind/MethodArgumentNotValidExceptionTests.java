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

package org.springframework.web.bind;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringValidatorAdapter;
import org.springframework.web.util.BindErrorUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MethodArgumentNotValidException}.
 *
 * @author Rossen Stoyanchev
 */
class MethodArgumentNotValidExceptionTests {

	@Test
	void errorsToStringList() throws Exception {
		Person frederick1234 = new Person("Frederick1234", 24);
		MethodArgumentNotValidException ex = createException(frederick1234);

		Collection<String> errors = BindErrorUtils.resolve(ex.getFieldErrors()).values();

		assertThat(errors).containsExactlyInAnyOrder(
				"name: size must be between 0 and 10", "age: must be greater than or equal to 25");
	}

	@Test
	void errorsToStringListWithMessageSource() throws Exception {
		Person frederick1234 = new Person("Frederick1234", 24);
		MethodArgumentNotValidException ex = createException(frederick1234);

		StaticMessageSource source = new StaticMessageSource();
		source.addMessage("Size.name", Locale.UK, "name exceeds {1} characters");
		source.addMessage("Min.age", Locale.UK, "age is under {1}");

		Collection<String> errors = BindErrorUtils.resolve(ex.getFieldErrors(), source, Locale.UK).values();

		assertThat(errors).containsExactlyInAnyOrder("name exceeds 10 characters", "age is under 25");
	}

	private static MethodArgumentNotValidException createException(Person person) throws Exception {
		LocaleContextHolder.setLocale(Locale.UK);
		try {
			LocalValidatorFactoryBean validatorBean = new LocalValidatorFactoryBean();
			validatorBean.afterPropertiesSet();
			SpringValidatorAdapter validator = new SpringValidatorAdapter(validatorBean);

			BindingResult result = new BeanPropertyBindingResult(person, "person");
			validator.validate(person, result);

			Method method = Handler.class.getDeclaredMethod("handle", Person.class);
			MethodParameter parameter = new MethodParameter(method, 0);

			return new MethodArgumentNotValidException(parameter, result);
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}


	@SuppressWarnings("unused")
	private static class Handler {

		@SuppressWarnings("unused")
		void handle(Person person) {
		}
	}


	@SuppressWarnings("unused")
	private record Person(@Size(max = 10) String name, @Min(25) int age) {
	}

}
