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

package org.springframework.validation.beanvalidation;

import java.util.Locale;
import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.validation.method.ParameterErrors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for method validation proxy with reactor.
 *
 * @author Rossen Stoyanchev
 */
class MethodValidationProxyReactorTests {

	@Test
	void validMonoArgument() {
		MyService myService = initProxy(new MyService(), false);
		Mono<Person> personMono = Mono.just(new Person("Faustino1234"));

		StepVerifier.create(myService.addPerson(personMono))
				.expectErrorSatisfies(t -> {
					ConstraintViolationException ex = (ConstraintViolationException) t;
					Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
					assertThat(violations).hasSize(1);
					assertThat(violations.iterator().next().getMessage()).isEqualTo("size must be between 1 and 10");
				})
				.verify();
	}

	@Test
	void validFluxArgument() {
		MyService myService = initProxy(new MyService(), false);
		Flux<Person> personFlux = Flux.just(new Person("Faust"), new Person("Faustino1234"));

		StepVerifier.create(myService.addPersons(personFlux))
				.expectErrorSatisfies(t -> {
					ConstraintViolationException ex = (ConstraintViolationException) t;
					Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
					assertThat(violations).hasSize(1);
					assertThat(violations.iterator().next().getMessage()).isEqualTo("size must be between 1 and 10");
				})
				.verify();
	}

	@Test
	void validMonoArgumentWithAdaptedViolations() {
		MyService myService = initProxy(new MyService(), true);
		Mono<Person> personMono = Mono.just(new Person("Faustino1234"));

		StepVerifier.create(myService.addPerson(personMono))
				.expectErrorSatisfies(t -> {
					MethodValidationException ex = (MethodValidationException) t;
					assertThat(ex.getParameterValidationResults()).hasSize(1);

					ParameterErrors errors = ex.getBeanResults().get(0);
					assertThat(errors.getErrorCount()).isEqualTo(1);
					assertThat(errors.getFieldErrors().get(0).toString()).isEqualTo("""
							Field error in object 'Person' on field 'name': rejected value [Faustino1234]; \
							codes [Size.Person.name,Size.name,Size.java.lang.String,Size]; \
							arguments [org.springframework.context.support.DefaultMessageSourceResolvable: \
							codes [Person.name,name]; arguments []; default message [name],10,1]; \
							default message [size must be between 1 and 10]""");
				})
				.verify();
	}

	private static MyService initProxy(Object target, boolean adaptViolations) {
		Locale oldDefault = Locale.getDefault();
		Locale.setDefault(Locale.US);
		try {
			Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
			MethodValidationInterceptor interceptor = new MethodValidationInterceptor(() -> validator, adaptViolations);
			ProxyFactory factory = new ProxyFactory(target);
			factory.addAdvice(interceptor);
			return (MyService) factory.getProxy();
		}
		finally {
			Locale.setDefault(oldDefault);
		}
	}


	@SuppressWarnings("unused")
	static class MyService {

		public Mono<Void> addPerson(@Valid Mono<Person> personMono) {
			return personMono.then();
		}

		public Mono<Void> addPersons(@Valid Flux<Person> personFlux) {
			return personFlux.then();
		}
	}


	@SuppressWarnings("unused")
	record Person(@Size(min = 1, max = 10) String name) {
	}

}
