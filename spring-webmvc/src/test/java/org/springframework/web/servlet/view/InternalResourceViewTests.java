/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockRequestDispatcher;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.servlet.View;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Unit tests for {@link InternalResourceView}.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class InternalResourceViewTests {

	@SuppressWarnings("serial")
	private static final Map<String, Object> model = Collections.unmodifiableMap(new HashMap<String, Object>() {{
		put("foo", "bar");
		put("I", 1L);
	}});

	private static final String url = "forward-to";

	private final HttpServletRequest request = mock(HttpServletRequest.class);

	private final MockHttpServletResponse response = new MockHttpServletResponse();

	private final InternalResourceView view = new InternalResourceView();


	/**
	 * If the url property isn't supplied, view initialization should fail.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullUrl() throws Exception {
		view.afterPropertiesSet();
	}

	@Test
	public void forward() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/myservlet/handler.do");
		request.setContextPath("/mycontext");
		request.setServletPath("/myservlet");
		request.setPathInfo(";mypathinfo");
		request.setQueryString("?param1=value1");

		view.setUrl(url);
		view.setServletContext(new MockServletContext() {
			@Override
			public int getMinorVersion() {
				return 4;
			}
		});

		view.render(model, request, response);
		assertEquals(url, response.getForwardedUrl());

		model.keySet().stream().forEach(
			key -> assertEquals("Values for model key '" + key + "' must match", model.get(key), request.getAttribute(key))
		);
	}

	@Test
	public void alwaysInclude() throws Exception {
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		view.setUrl(url);
		view.setAlwaysInclude(true);

		// Can now try multiple tests
		view.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());

		model.keySet().stream().forEach(key -> verify(request).setAttribute(key, model.get(key)));
	}

	@Test
	public void includeOnAttribute() throws Exception {
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn("somepath");
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		view.setUrl(url);

		// Can now try multiple tests
		view.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());

		model.keySet().stream().forEach(key -> verify(request).setAttribute(key, model.get(key)));
	}

	@Test
	public void includeOnCommitted() throws Exception {
		given(request.getAttribute(View.PATH_VARIABLES)).willReturn(null);
		given(request.getAttribute(WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE)).willReturn(null);
		given(request.getRequestDispatcher(url)).willReturn(new MockRequestDispatcher(url));

		response.setCommitted(true);
		view.setUrl(url);

		// Can now try multiple tests
		view.render(model, request, response);
		assertEquals(url, response.getIncludedUrl());

		model.keySet().stream().forEach(key -> verify(request).setAttribute(key, model.get(key)));
	}

}
