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

package org.springframework.web.servlet.mvc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with a ParameterizableViewController.
 *
 * @author Rossen Stoyanchev
 * @since 3.1.1
 */
class ParameterizableViewControllerTests {

	private ParameterizableViewController controller;

	private MockHttpServletRequest request;

	@BeforeEach
	void setup() {
		this.controller = new ParameterizableViewController();
		this.request = new MockHttpServletRequest("GET", "/");
	}

	@Test
	void handleRequestWithViewName() throws Exception {
		String viewName = "testView";
		this.controller.setViewName(viewName);
		ModelAndView mav = this.controller.handleRequest(this.request, new MockHttpServletResponse());
		assertThat(mav.getViewName()).isEqualTo(viewName);
		assertThat(mav.getModel()).isEmpty();
	}

	@Test
	void handleRequestWithoutViewName() throws Exception {
		ModelAndView mav = this.controller.handleRequest(this.request, new MockHttpServletResponse());
		assertThat(mav.getViewName()).isNull();
		assertThat(mav.getModel()).isEmpty();
	}

	@Test
	void handleRequestWithFlashAttributes() throws Exception {
		this.request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, new ModelMap("name", "value"));
		ModelAndView mav = this.controller.handleRequest(this.request, new MockHttpServletResponse());
		assertThat(mav.getModel()).hasSize(1);
		assertThat(mav.getModel().get("name")).isEqualTo("value");
	}

	@Test
	void handleRequestHttpOptions() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = this.controller.handleRequest(this.request, response);

		assertThat(mav).isNull();
		assertThat(response.getHeader("Allow")).isEqualTo("GET,HEAD,OPTIONS");
	}

}
