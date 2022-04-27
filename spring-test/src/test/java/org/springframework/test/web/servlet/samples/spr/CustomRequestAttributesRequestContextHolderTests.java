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

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotatedBeanDefinitionReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

/**
 * Integration tests for SPR-13211 which verify that a custom mock request
 * is not reused by MockMvc.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see RequestContextHolderTests
 */
public class CustomRequestAttributesRequestContextHolderTests {

	private static final String FROM_CUSTOM_MOCK = "fromCustomMock";
	private static final String FROM_MVC_TEST_DEFAULT = "fromSpringMvcTestDefault";
	private static final String FROM_MVC_TEST_MOCK = "fromSpringMvcTestMock";

	private final GenericWebApplicationContext wac = new GenericWebApplicationContext();

	private MockMvc mockMvc;


	@BeforeEach
	public void setUp() {
		ServletContext servletContext = new MockServletContext();
		MockHttpServletRequest mockRequest = new MockHttpServletRequest(servletContext);
		mockRequest.setAttribute(FROM_CUSTOM_MOCK, FROM_CUSTOM_MOCK);
		RequestContextHolder.setRequestAttributes(new ServletWebRequest(mockRequest, new MockHttpServletResponse()));

		this.wac.setServletContext(servletContext);
		new AnnotatedBeanDefinitionReader(this.wac).register(WebConfig.class);
		this.wac.refresh();

		this.mockMvc = webAppContextSetup(this.wac)
				.defaultRequest(get("/").requestAttr(FROM_MVC_TEST_DEFAULT, FROM_MVC_TEST_DEFAULT))
				.alwaysExpect(status().isOk())
				.build();
	}

	@Test
	public void singletonController() throws Exception {
		this.mockMvc.perform(get("/singletonController").requestAttr(FROM_MVC_TEST_MOCK, FROM_MVC_TEST_MOCK));
	}

	@AfterEach
	public void verifyCustomRequestAttributesAreRestored() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes).isInstanceOf(ServletRequestAttributes.class);
		HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

		assertThat(request.getAttribute(FROM_CUSTOM_MOCK)).isEqualTo(FROM_CUSTOM_MOCK);
		assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isNull();
		assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isNull();

		RequestContextHolder.resetRequestAttributes();
		this.wac.close();
	}


	// -------------------------------------------------------------------

	@Configuration
	@EnableWebMvc
	static class WebConfig implements WebMvcConfigurer {

		@Bean
		public SingletonController singletonController() {
			return new SingletonController();
		}
	}

	@RestController
	private static class SingletonController {

		@RequestMapping("/singletonController")
		public void handle() {
			assertRequestAttributes();
		}
	}

	private static void assertRequestAttributes() {
		RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
		assertThat(requestAttributes).isInstanceOf(ServletRequestAttributes.class);
		assertRequestAttributes(((ServletRequestAttributes) requestAttributes).getRequest());
	}

	private static void assertRequestAttributes(ServletRequest request) {
		assertThat(request.getAttribute(FROM_CUSTOM_MOCK)).isNull();
		assertThat(request.getAttribute(FROM_MVC_TEST_DEFAULT)).isEqualTo(FROM_MVC_TEST_DEFAULT);
		assertThat(request.getAttribute(FROM_MVC_TEST_MOCK)).isEqualTo(FROM_MVC_TEST_MOCK);
	}

}
