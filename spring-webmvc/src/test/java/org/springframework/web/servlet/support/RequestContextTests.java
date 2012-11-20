/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.web.servlet.support;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

/**
 * @author Dave Syer
 * @author Rossen Stoyanchev
 */
public class RequestContextTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private MockServletContext servletContext = new MockServletContext();

	private Map<String, Object> model = new HashMap<String, Object>();

	@Before
	public void init() {
		GenericWebApplicationContext applicationContext = new GenericWebApplicationContext();
		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);
	}

	@Test
	public void testGetContextUrl() throws Exception {
		request.setContextPath("foo/");
		RequestContext context = new RequestContext(request, response, servletContext, model);
		assertEquals("foo/bar", context.getContextUrl("bar"));
	}

	@Test
	public void testGetContextUrlWithMap() throws Exception {
		request.setContextPath("foo/");
		RequestContext context = new RequestContext(request, response, servletContext, model);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("foo", "bar");
		map.put("spam", "bucket");
		assertEquals("foo/bar?spam=bucket", context.getContextUrl("{foo}?spam={spam}", map));
	}
	
	@Test
	public void testGetContextUrlWithMapEscaping() throws Exception {
		request.setContextPath("foo/");
		RequestContext context = new RequestContext(request, response, servletContext, model);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("foo", "bar baz");
		map.put("spam", "&bucket=");
		assertEquals("foo/bar%20baz?spam=%26bucket%3D", context.getContextUrl("{foo}?spam={spam}", map));
	}

	@Test
	public void testPathToServlet() throws Exception {
		request.setContextPath("/app");
		request.setServletPath("/servlet");
		RequestContext context = new RequestContext(request, response, servletContext, model);
		
		assertEquals("/app/servlet", context.getPathToServlet());

		request.setAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE, "/origApp");
		request.setAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE, "/origServlet");
		
		assertEquals("/origApp/origServlet", context.getPathToServlet());
	}

}
