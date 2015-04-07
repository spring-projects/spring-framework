/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.mvc.support;

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.mvc.ParameterizableViewController;
import org.springframework.web.servlet.view.RedirectView;

import static org.junit.Assert.*;

/**
 * Unit tests for
 * {@link org.springframework.web.servlet.mvc.ParameterizableViewController}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
public class ParameterizableViewControllerTests {

	private ParameterizableViewController controller;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;


	@Before
	public void setUp() throws Exception {
		this.controller = new ParameterizableViewController();
		this.request = new MockHttpServletRequest("GET", "/");
		this.response = new MockHttpServletResponse();
	}


	@Test
	public void defaultViewName() throws Exception {
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertNull(modelAndView.getViewName());
	}

	@Test
	public void viewName() throws Exception {
		this.controller.setViewName("view");
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertEquals("view", modelAndView.getViewName());
	}

	@Test
	public void viewNameAndStatus() throws Exception {
		this.controller.setViewName("view");
		this.controller.setStatusCode(HttpStatus.NOT_FOUND);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertEquals("view", modelAndView.getViewName());
		assertEquals(404, this.response.getStatus());
	}

	@Test
	public void viewNameAndStatus204() throws Exception {
		this.controller.setStatusCode(HttpStatus.NO_CONTENT);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertNull(modelAndView);
		assertEquals(204, this.response.getStatus());
	}

	@Test
	public void redirectStatus() throws Exception {
		this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
		this.controller.setViewName("/foo");
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);

		assertEquals("redirect:/foo", modelAndView.getViewName());
		assertEquals("3xx status should be left to RedirectView to set", 200, this.response.getStatus());
		assertEquals(HttpStatus.PERMANENT_REDIRECT, this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE));
	}

	@Test
	public void redirectStatusWithRedirectPrefix() throws Exception {
		this.controller.setStatusCode(HttpStatus.PERMANENT_REDIRECT);
		this.controller.setViewName("redirect:/foo");
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);

		assertEquals("redirect:/foo", modelAndView.getViewName());
		assertEquals("3xx status should be left to RedirectView to set", 200, this.response.getStatus());
		assertEquals(HttpStatus.PERMANENT_REDIRECT, this.request.getAttribute(View.RESPONSE_STATUS_ATTRIBUTE));
	}

	@Test
	public void redirectView() throws Exception {
		RedirectView view = new RedirectView("/foo");
		this.controller.setView(view);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertSame(view, modelAndView.getView());
	}

	@Test
	public void statusOnly() throws Exception {
		this.controller.setStatusCode(HttpStatus.NOT_FOUND);
		this.controller.setStatusOnly(true);
		ModelAndView modelAndView = this.controller.handleRequest(this.request, this.response);
		assertNull(modelAndView);
		assertEquals(404, this.response.getStatus());
	}

}
