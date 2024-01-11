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

package org.springframework.web.servlet.mvc.support;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ParameterizableViewController}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class ParameterizableViewControllerTests {

	private final ParameterizableViewController controller = new ParameterizableViewController();

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	void defaultViewName() throws Exception {
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertThat(modelAndView.getViewName()).isNull();
	}

	@Test
	void viewName() throws Exception {
		this.controller.setViewName("view");
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertThat(modelAndView.getViewName()).isEqualTo("view");
	}

	@Test
	void viewNameAndStatus() throws Exception {
		this.controller.setViewName("view");
		this.controller.setStatusCode(HttpStatus.NOT_FOUND);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertThat(modelAndView.getViewName()).isEqualTo("view");
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

	@Test
	void viewNameAndStatus204() throws Exception {
		this.controller.setStatusCode(HttpStatus.NO_CONTENT);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertThat(modelAndView).isNull();
		assertThat(this.response.getStatus()).isEqualTo(204);
	}

	@Test
	void redirectStatus() throws Exception {
		this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
		this.controller.setViewName("/foo");
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);

		assertThat(modelAndView.getViewName()).isEqualTo("redirect:/foo");
		assertThat(this.response.getStatus()).as("3xx status should be left to RedirectView to set").isEqualTo(200);
		assertThat(this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE)).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
	}

	@Test
	void redirectStatusWithRedirectPrefix() throws Exception {
		this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
		this.controller.setViewName("redirect:/foo");
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);

		assertThat(modelAndView.getViewName()).isEqualTo("redirect:/foo");
		assertThat(this.response.getStatus()).as("3xx status should be left to RedirectView to set").isEqualTo(200);
		assertThat(this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE)).isEqualTo(HttpStatus.PERMANENT_REDIRECT);
	}

	@Test
	void redirectView() throws Exception {
		RedirectView view = new RedirectView("/foo");
		this.controller.setView(view);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertThat(modelAndView.getView()).isSameAs(view);
	}

	@Test
	void statusOnly() throws Exception {
		this.controller.setStatusCode(HttpStatus.NOT_FOUND);
		this.controller.setStatusOnly(true);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertThat(modelAndView).isNull();
		assertThat(this.response.getStatus()).isEqualTo(404);
	}

}
