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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.annotation.RequestScope;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.junit.Assert.assertEquals;
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
@RunWith(SpringRunner.class)
@WebAppConfiguration
public class ControllerAdviceIntegrationTests {

	@Autowired
	WebApplicationContext wac;

	MockMvc mockMvc;

	@Before
	public void setUpMockMvc() {
		this.mockMvc = webAppContextSetup(wac).build();
	}

	@Test
	public void controllerAdviceIsAppliedOnlyOnce() throws Exception {
		assertEquals(0, SingletonControllerAdvice.counter.get());
		assertEquals(0, RequestScopedControllerAdvice.counter.get());

		this.mockMvc.perform(get("/test"))//
				.andExpect(status().isOk())//
				.andExpect(forwardedUrl("singleton:1;request-scoped:1"));

		assertEquals(1, SingletonControllerAdvice.counter.get());
		assertEquals(1, RequestScopedControllerAdvice.counter.get());
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

		@ModelAttribute
		void initModel(Model model) {
			model.addAttribute("singleton", counter.incrementAndGet());
		}
	}

	@ControllerAdvice
	static class RequestScopedControllerAdvice {

		static final AtomicInteger counter = new AtomicInteger();

		@ModelAttribute
		void initModel(Model model) {
			model.addAttribute("request-scoped", counter.incrementAndGet());
		}
	}

	@Controller
	static class TestController {

		@GetMapping("/test")
		String get(Model model) {
			return "singleton:" + model.asMap().get("singleton") + ";request-scoped:"
					+ model.asMap().get("request-scoped");
		}
	}

}
