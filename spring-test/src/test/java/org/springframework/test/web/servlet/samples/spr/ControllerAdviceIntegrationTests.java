/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.test.web.servlet.samples.spr;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests for {@link ControllerAdvice @ControllerAdvice}.
 *
 * <p>Introduced in conjunction with
 * <a href="https://github.com/spring-projects/spring-framework/issues/24017">gh-24017</a>.
 *
 * @author Sam Brannen
 * @since 5.1.12
 */
@SpringJUnitWebConfig
class ControllerAdviceIntegrationTests {

	MockMvc mockMvc;

	@BeforeEach
	void setUpMockMvc(WebApplicationContext wac) {
		this.mockMvc = webAppContextSetup(wac).build();
		resetCounters();
	}

	@Test
	void controllerAdviceIsAppliedOnlyOnce() throws Exception {
		assertThat(SingletonControllerAdvice.counter).hasValue(0);
		assertThat(RequestScopedControllerAdvice.counter).hasValue(0);

		this.mockMvc.perform(get("/test").param("requestParam", "foo"))//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("singleton:1;request-scoped:1;requestParam:foo"));

		assertThat(SingletonControllerAdvice.counter).hasValue(1);
		assertThat(RequestScopedControllerAdvice.counter).hasValue(1);
	}

	@Test
	void requestScopedControllerAdviceBeanIsNotCached() throws Exception {
		assertThat(SingletonControllerAdvice.instanceCounter).hasValue(0);
		assertThat(RequestScopedControllerAdvice.instanceCounter).hasValue(0);

		this.mockMvc.perform(get("/test").param("requestParam", "foo"))//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("singleton:1;request-scoped:1;requestParam:foo"));

		// A singleton @ControllerAdvice bean should not be instantiated again.
		assertThat(SingletonControllerAdvice.instanceCounter).hasValue(0);
		// A request-scoped @ControllerAdvice bean should be instantiated once per request.
		assertThat(RequestScopedControllerAdvice.instanceCounter).hasValue(1);

		this.mockMvc.perform(get("/test").param("requestParam", "bar"))//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("singleton:2;request-scoped:2;requestParam:bar"));

		// A singleton @ControllerAdvice bean should not be instantiated again.
		assertThat(SingletonControllerAdvice.instanceCounter).hasValue(0);
		// A request-scoped @ControllerAdvice bean should be instantiated once per request.
		assertThat(RequestScopedControllerAdvice.instanceCounter).hasValue(2);
	}

	private void resetCounters() {
		SingletonControllerAdvice.counter.set(0);
		SingletonControllerAdvice.instanceCounter.set(0);
		RequestScopedControllerAdvice.counter.set(0);
		RequestScopedControllerAdvice.instanceCounter.set(0);
	}

	@Configuration
	@EnableWebMvc
	static class Config {

		@Bean
		TestController testController() {
			return new TestController();
		}

		@Bean
		SingletonControllerAdvice singletonControllerAdvice() {
			return new SingletonControllerAdvice();
		}

		@Bean
		@RequestScope
		RequestScopedControllerAdvice requestScopedControllerAdvice() {
			return new RequestScopedControllerAdvice();
		}
	}

	@ControllerAdvice
	static class SingletonControllerAdvice {

		static final AtomicInteger counter = new AtomicInteger();

		static final AtomicInteger instanceCounter = new AtomicInteger();

		{
			instanceCounter.incrementAndGet();
		}

		@ModelAttribute
		void initModel(Model model) {
			model.addAttribute("singleton", counter.incrementAndGet());
		}
	}

	@ControllerAdvice
	static class RequestScopedControllerAdvice {

		static final AtomicInteger counter = new AtomicInteger();

		static final AtomicInteger instanceCounter = new AtomicInteger();

		{
			instanceCounter.incrementAndGet();
		}

		@ModelAttribute
		void initModel(@RequestParam String requestParam, Model model) {
			model.addAttribute("requestParam", requestParam);
			model.addAttribute("request-scoped", counter.incrementAndGet());
		}
	}

	@Controller
	static class TestController {

		@GetMapping("/test")
		String get(Model model) {
			return "singleton:" + model.getAttribute("singleton") + ";request-scoped:"
					+ model.getAttribute("request-scoped") + ";requestParam:"
					+ model.getAttribute("requestParam");
		}
	}

}
