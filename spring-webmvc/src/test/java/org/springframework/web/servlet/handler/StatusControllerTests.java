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

package org.springframework.web.servlet.handler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.StatusController;

/**
 * Test fixture for {@link StatusControllerTests} tests.
 *
 * @author Sebastien Deleuze
 * @since 4.1
 */
public class StatusControllerTests {

	@Test
	public void status() throws Exception {
		StatusController controller = new StatusController(HttpStatus.NOT_ACCEPTABLE);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), response.getStatus());
		assertNull(mav);
	}

	@Test
	public void statusAndView() throws Exception {
		StatusController controller = new StatusController(HttpStatus.NOT_ACCEPTABLE);
		controller.setViewName("view1");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), response.getStatus());
		assertNotNull(mav);
	}

	@Test
	public void statusAndReason() throws Exception {
		StatusController controller = new StatusController(HttpStatus.NOT_ACCEPTABLE);
		controller.setReason("Foo");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertEquals(HttpStatus.NOT_ACCEPTABLE.value(), response.getStatus());
		assertNull(mav);
		assertEquals("Foo", response.getErrorMessage());
	}

	@Test(expected = IllegalStateException.class)
	public void viewAndReason() throws Exception {
		StatusController controller = new StatusController(HttpStatus.NOT_ACCEPTABLE);
		controller.setViewName("view1");
		controller.setReason("Foo");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/test");
		MockHttpServletResponse response = new MockHttpServletResponse();
		controller.handleRequest(request, response);
		fail();
	}

	@Test
	public void redirect() throws Exception {
		StatusController controller = new StatusController(HttpStatus.TEMPORARY_REDIRECT);
		controller.setRedirectPath("/destination");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/source");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertEquals(HttpStatus.TEMPORARY_REDIRECT.value(), response.getStatus());
		assertNull(mav);
		assertEquals("/destination", response.getHeader(HttpHeaders.LOCATION));
	}

	@Test
	public void redirectWithoutQueryString() throws Exception {
		StatusController controller = new StatusController(HttpStatus.TEMPORARY_REDIRECT);
		controller.setRedirectPath("/destination");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/source");
		request.setQueryString("foo=bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertEquals(HttpStatus.TEMPORARY_REDIRECT.value(), response.getStatus());
		assertNull(mav);
		assertEquals("/destination", response.getHeader(HttpHeaders.LOCATION));
	}

	@Test
	public void redirectWithQueryString() throws Exception {
		StatusController controller = new StatusController(HttpStatus.TEMPORARY_REDIRECT);
		controller.setRedirectPath("/destination");
		controller.setUseQueryString(true);
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/source?foo=bar");
		request.setQueryString("foo=bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		ModelAndView mav = controller.handleRequest(request, response);
		assertEquals(HttpStatus.TEMPORARY_REDIRECT.value(), response.getStatus());
		assertNull(mav);
		assertEquals("/destination?foo=bar", response.getHeader(HttpHeaders.LOCATION));
	}

	@Test(expected = IllegalStateException.class)
	public void redirectWithWrongStatusCode() throws Exception {
		StatusController controller = new StatusController(HttpStatus.NOT_ACCEPTABLE);
		controller.setRedirectPath("/destination");
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/source?foo=bar");
		MockHttpServletResponse response = new MockHttpServletResponse();
		controller.handleRequest(request, response);
		fail();
	}


}
