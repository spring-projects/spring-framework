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

package org.springframework.test.web.client.samples;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests that use a {@link RestTemplate} configured with a
 * {@link MockMvcClientHttpRequestFactory} that is in turn configured with a
 * {@link MockMvc} instance that uses a {@link WebApplicationContext} loaded by
 * the TestContext framework.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration
public class MockMvcClientHttpRequestFactoryTests {

	@Autowired
	private WebApplicationContext wac;

	private RestTemplate template;


	@BeforeEach
	public void setup() {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
		this.template = new RestTemplate(new MockMvcClientHttpRequestFactory(mockMvc));
	}

	@Test
	public void withResult() {
		assertThat(template.getForObject("/foo", String.class)).isEqualTo("bar");
	}

	@Test
	public void withError() {
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> template.getForEntity("/error", String.class))
				.withMessageContaining("400")
				.withMessageContaining("some bad request");
	}

	@Test
	public void withErrorAndBody() {
		assertThatExceptionOfType(HttpClientErrorException.class)
				.isThrownBy(() -> template.getForEntity("/errorbody", String.class))
				.withMessageContaining("400")
				.withMessageContaining("some really bad request");
	}


	@EnableWebMvc
	@Configuration
	@ComponentScan(basePackageClasses = MockMvcClientHttpRequestFactoryTests.class)
	static class MyWebConfig implements WebMvcConfigurer {
	}

	@Controller
	static class MyController {

		@RequestMapping(value = "/foo", method = RequestMethod.GET)
		@ResponseBody
		public String handle() {
			return "bar";
		}

		@RequestMapping(value = "/error", method = RequestMethod.GET)
		public void handleError(HttpServletResponse response) throws Exception {
			response.sendError(400, "some bad request");
		}

		@RequestMapping(value = "/errorbody", method = RequestMethod.GET)
		public void handleErrorWithBody(HttpServletResponse response) throws Exception {
			response.sendError(400, "some bad request");
			response.getWriter().write("some really bad request");
		}
	}

}
