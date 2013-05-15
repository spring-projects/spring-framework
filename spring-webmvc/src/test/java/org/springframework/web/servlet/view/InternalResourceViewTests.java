/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.servlet.view;

import java.util.HashMap;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockRequestDispatcher;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.servlet.View;
import org.springframework.web.util.WebUtils;

import static org.mockito.BDDMockito.*;

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class InternalResourceViewTests extends TestCase {

	/**
	 * Test that if the url property isn't supplied, view initialization fails.
	 */
	public void testRejectsNullUrl() throws Exception {
		InternalResourceView view = new InternalResourceView();
		try {
			view.afterPropertiesSet();
			fail("Should be forced to set URL");
		}
		catch (IllegalArgumentException ex) {
			// expected
		}
	}

	public void testForward() throws Exception {
		HashMap<String, Object> model = new HashMap<String, Object>();
		Object obj = 1;
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myservlet/handler.do");
		request.setContextPath("/mycontext");
		request.setServletPath("/myservlet");
		request.setPathInfo(";mypathinfo");
		request.setQueryString("?param1=value1");

		InternalResourceView view = new InternalResourceView();
		view.setUrl(url);
		view.setServletContext(new MockServletContext() {
			@Override
			public int getMinorVersion() {
				return 4;
			}
		});

		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(model, request, response);
		assertEquals(url, response.getForwardedUrl());

		Set<String> keys = model.keySet();
		for (String key : keys) {
			assertEquals(model.get(key), request.getAttribute(key));
		}
	}

	public void testAlwaysInclude() throws Exception {
		HashMap<String, Object> model = new HashMap<String, Object>();
		Object obj = 1;
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		MockHttpServletResponse response = new MockHttpServletResponse();
		InternalResourceView v = new InternalResourceView();
		v.setUrl(url);
		v.setAlwaysInclude(true);

		// Can now try multiple tests
		v.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());

		Set<String> keys = model.keySet();
		for (String key : keys) {
			verify(request).setAttribute(key, model.get(key));
		}
	}

	public void testIncludeOnAttribute() throws Exception {
		HashMap<String, Object> model = new HashMap<String, Object>();
		Object obj = 1;
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);

		given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn("somepath");
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		MockHttpServletResponse response = new MockHttpServletResponse();
		InternalResourceView v = new InternalResourceView();
		v.setUrl(url);

		// Can now try multiple tests
		v.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());

		Set<String> keys = model.keySet();
		for (String key : keys) {
			verify(request).setAttribute(key, model.get(key));
		}
	}

	public void testIncludeOnCommitted() throws Exception {
		HashMap<String, Object> model = new HashMap<String, Object>();
		Object obj = 1;
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		HttpServletRequest request = mock(HttpServletRequest.class);
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);

		given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setCommitted(true);
		InternalResourceView v = new InternalResourceView();
		v.setUrl(url);

		// Can now try multiple tests
		v.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());

		Set<String> keys = model.keySet();
		for (String key : keys) {
			verify(request).setAttribute(key, model.get(key));
		}
	}

}
