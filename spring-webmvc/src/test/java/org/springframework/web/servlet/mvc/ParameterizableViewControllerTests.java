/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;

/**
 * Test fixture with a ParameterizableViewController.
 *
 * @author Rossen Stoyanchev
 * @since 3.1.1
 */
public class ParameterizableViewControllerTests {

	private ParameterizableViewController controller;

	private MockHttpServletRequest request;

	@Before
	public void setup() {
		this.controller = new ParameterizableViewController();
		this.request = new MockHttpServletRequest("GET", "/");
	}

	@Test
	public void handleRequestWithViewName() throws Exception {
		String viewName = "testView";
		this.controller.setViewName(viewName);
		ModelAndView mav = this.controller.handleRequest(this.request, new MockHttpServletResponse());
		assertEquals(viewName, mav.getViewName());
		assertTrue(mav.getModel().isEmpty());
	}

	@Test
	public void handleRequestWithoutViewName() throws Exception {
		ModelAndView mav = this.controller.handleRequest(this.request, new MockHttpServletResponse());
		assertNull(mav.getViewName());
		assertTrue(mav.getModel().isEmpty());
	}

	@Test
	public void handleRequestWithFlashAttributes() throws Exception {
		this.request.setAttribute(DispatcherServlet.INPUT_FLASH_MAP_ATTRIBUTE, new ModelMap("name", "value"));
		ModelAndView mav = this.controller.handleRequest(this.request, new MockHttpServletResponse());
		assertEquals(1, mav.getModel().size());
		assertEquals("value", mav.getModel().get("name"));
	}

	@Test
	public void handleRequestHttpOptions() throws Exception {
		this.request.setMethod(HttpMethod.OPTIONS.name());
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = this.controller.handleRequest(this.request, response);

		assertNull(mav);
		assertEquals("GET,HEAD,OPTIONS", response.getHeader("Allow"));
	}

}
