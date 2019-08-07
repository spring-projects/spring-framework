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

package org.springframework.web.reactive.result.method.annotation;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import org.junit.Test;
import reactor.core.publisher.Mono;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.junit.Assert.*;

/**
 * Data binding and type conversion related integration tests for
 * {@code @Controller}-annotated classes.
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingDataBindingIntegrationTests extends AbstractRequestMappingIntegrationTests {

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}


	@Test
	public void handleDateParam() throws Exception {
		assertEquals("Processed date!",
				performPost("/date-param?date=2016-10-31&date-pattern=YYYY-mm-dd",
						new HttpHeaders(), null, String.class).getBody());
	}

	@Test
	public void handleForm() throws Exception {

		MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
		formData.add("name", "George");
		formData.add("age", "5");

		assertEquals("Processed form: Foo[id=1, name='George', age=5]",
				performPost("/foos/1", MediaType.APPLICATION_FORM_URLENCODED, formData,
						MediaType.TEXT_PLAIN, String.class).getBody());
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/RequestMappingDataBindingIntegrationTests*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}


	@RestController
	@SuppressWarnings({"unused", "OptionalUsedAsFieldOrParameterType"})
	private static class TestController {

		@InitBinder
		public void initBinder(WebDataBinder binder,
				@RequestParam("date-pattern") Optional<String> optionalPattern) {

			optionalPattern.ifPresent(pattern -> {
				CustomDateEditor dateEditor = new CustomDateEditor(new SimpleDateFormat(pattern), false);
				binder.registerCustomEditor(Date.class, dateEditor);
			});
		}

		@PostMapping("/date-param")
		public String handleDateParam(@RequestParam Date date) {
			return "Processed date!";
		}

		@ModelAttribute
		public Mono<Foo> addFooAttribute(@PathVariable("id") Optional<Long> optiponalId) {
			return optiponalId.map(id -> Mono.just(new Foo(id))).orElse(Mono.empty());
		}

		@PostMapping("/foos/{id}")
		public String handleForm(@ModelAttribute Foo foo, Errors errors) {
			return (errors.hasErrors() ?
					"Form not processed" : "Processed form: " + foo);
		}
	}


	@SuppressWarnings("unused")
	private static class Foo {

		private final Long id;

		private String name;

		private int age;

		public Foo(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return this.age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		@Override
		public String toString() {
			return "Foo[id=" + this.id + ", name='" + this.name + "', age=" + this.age + "]";
		}
	}

}
