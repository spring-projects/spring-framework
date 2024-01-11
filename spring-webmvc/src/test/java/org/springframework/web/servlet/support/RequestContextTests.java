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

package org.springframework.web.servlet.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Dave Syer
 * @author Rossen Stoyanchev
 */
class RequestContextTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private MockServletContext servletContext = new MockServletContext();

	private Map<String, Object> model = new HashMap<>();

	@BeforeEach
	void init() {
		GenericWebApplicationContext applicationContext = new GenericWebApplicationContext();
		applicationContext.refresh();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
	}

	@Test
	void testGetContextUrl() {
		request.setContextPath("foo/");
		RequestContext context = new RequestContext(request, response, servletContext, model);
		assertThat(context.getContextUrl("bar")).isEqualTo("foo/bar");
	}

	@Test
	void testGetContextUrlWithMap() {
		request.setContextPath("foo/");
		RequestContext context = new RequestContext(request, response, servletContext, model);
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("spam", "bucket");
		assertThat(context.getContextUrl("{foo}?spam={spam}", map)).isEqualTo("foo/bar?spam=bucket");
	}

	@Test
	void testGetContextUrlWithMapEscaping() {
		request.setContextPath("foo/");
		RequestContext context = new RequestContext(request, response, servletContext, model);
		Map<String, Object> map = new HashMap<>();
		map.put("foo", "bar baz");
		map.put("spam", "&bucket=");
		assertThat(context.getContextUrl("{foo}?spam={spam}", map)).isEqualTo("foo/bar%20baz?spam=%26bucket%3D");
	}

	@Test
	void testPathToServlet() {
		request.setContextPath("/app");
		request.setServletPath("/servlet");
		RequestContext context = new RequestContext(request, response, servletContext, model);

		assertThat(context.getPathToServlet()).isEqualTo("/app/servlet");

		request.setAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE, "/origApp");
		request.setAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE, "/origServlet");

		assertThat(context.getPathToServlet()).isEqualTo("/origApp/origServlet");
	}

}
