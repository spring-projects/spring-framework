/*
 * Copyright 2002-2008 the original author or authors.
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
import java.util.Iterator;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockRequestDispatcher;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.util.WebUtils;

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
		HashMap model = new HashMap();
		Object obj = new Integer(1);
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
			public int getMinorVersion() {
				return 4;
			}
		});

		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(model, request, response);
		assertEquals(url, response.getForwardedUrl());

		Set keys = model.keySet();
		for (Iterator it = keys.iterator(); it.hasNext();) {
			String key = (String) it.next();
			assertEquals(model.get(key), request.getAttribute(key));
		}

		assertEquals("/myservlet/handler.do", request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE));
		assertEquals("/mycontext", request.getAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE));
		assertEquals("/myservlet", request.getAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE));
		assertEquals(";mypathinfo", request.getAttribute(WebUtils.FORWARD_PATH_INFO_ATTRIBUTE));
		assertEquals("?param1=value1", request.getAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE));
	}

	public void testForwardWithForwardAttributesPresent() throws Exception {
		HashMap model = new HashMap();
		Object obj = new Integer(1);
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myservlet/handler.do");
		request.setContextPath("/mycontext");
		request.setServletPath("/myservlet");
		request.setPathInfo(";mypathinfo");
		request.setQueryString("?param1=value1");

		request.setAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE, "/MYservlet/handler.do");
		request.setAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE, "/MYcontext");
		request.setAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE, "/MYservlet");
		request.setAttribute(WebUtils.FORWARD_PATH_INFO_ATTRIBUTE, ";MYpathinfo");
		request.setAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE, "?Param1=value1");

		InternalResourceView view = new InternalResourceView();
		view.setUrl(url);
		view.setServletContext(new MockServletContext() {
			public int getMinorVersion() {
				return 4;
			}
		});

		MockHttpServletResponse response = new MockHttpServletResponse();
		view.render(model, request, response);
		assertEquals(url, response.getForwardedUrl());

		Set keys = model.keySet();
		for (Iterator it = keys.iterator(); it.hasNext();) {
			String key = (String) it.next();
			assertEquals(model.get(key), request.getAttribute(key));
		}

		assertEquals("/MYservlet/handler.do", request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE));
		assertEquals("/MYcontext", request.getAttribute(WebUtils.FORWARD_CONTEXT_PATH_ATTRIBUTE));
		assertEquals("/MYservlet", request.getAttribute(WebUtils.FORWARD_SERVLET_PATH_ATTRIBUTE));
		assertEquals(";MYpathinfo", request.getAttribute(WebUtils.FORWARD_PATH_INFO_ATTRIBUTE));
		assertEquals("?Param1=value1", request.getAttribute(WebUtils.FORWARD_QUERY_STRING_ATTRIBUTE));
	}

	public void testAlwaysInclude() throws Exception {
		HashMap model = new HashMap();
		Object obj = new Integer(1);
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		MockControl reqControl = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) reqControl.getMock();
		Set keys = model.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			request.setAttribute(key, model.get(key));
			reqControl.setVoidCallable(1);
		}

		request.getRequestDispatcher(url);
		reqControl.setReturnValue(new MockRequestDispatcher(url));
		reqControl.replay();

		MockHttpServletResponse response = new MockHttpServletResponse();
		InternalResourceView v = new InternalResourceView();
		v.setUrl(url);
		v.setAlwaysInclude(true);

		// Can now try multiple tests
		v.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());
		reqControl.verify();
	}

	public void testIncludeOnAttribute() throws Exception {
		HashMap model = new HashMap();
		Object obj = new Integer(1);
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		MockControl reqControl = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) reqControl.getMock();
		Set keys = model.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			request.setAttribute(key, model.get(key));
			reqControl.setVoidCallable(1);
		}

		request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		reqControl.setReturnValue("somepath");
		request.getRequestDispatcher(url);
		reqControl.setReturnValue(new MockRequestDispatcher(url));
		reqControl.replay();

		MockHttpServletResponse response = new MockHttpServletResponse();
		InternalResourceView v = new InternalResourceView();
		v.setUrl(url);

		// Can now try multiple tests
		v.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());
		reqControl.verify();
	}

	public void testIncludeOnCommitted() throws Exception {
		HashMap model = new HashMap();
		Object obj = new Integer(1);
		model.put("foo", "bar");
		model.put("I", obj);

		String url = "forward-to";

		MockControl reqControl = MockControl.createControl(HttpServletRequest.class);
		HttpServletRequest request = (HttpServletRequest) reqControl.getMock();
		Set keys = model.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			String key = (String) iter.next();
			request.setAttribute(key, model.get(key));
			reqControl.setVoidCallable(1);
		}

		request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE);
		reqControl.setReturnValue(null);
		request.getRequestDispatcher(url);
		reqControl.setReturnValue(new MockRequestDispatcher(url));
		reqControl.replay();

		MockHttpServletResponse response = new MockHttpServletResponse();
		response.setCommitted(true);
		InternalResourceView v = new InternalResourceView();
		v.setUrl(url);

		// Can now try multiple tests
		v.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());
		reqControl.verify();
	}

}
