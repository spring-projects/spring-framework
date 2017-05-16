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

package org.springframework.web.filter;

import java.io.IOException;
import java.util.Arrays;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Test;

import org.springframework.mock.web.test.MockFilterConfig;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;

import static org.junit.Assert.*;

/**
 * @author Dave Syer
 */
public class CompositeFilterTests {

	@Test
	public void testCompositeFilter() throws ServletException, IOException {
		ServletContext sc = new MockServletContext();
		MockFilter targetFilter = new MockFilter();
		MockFilterConfig proxyConfig = new MockFilterConfig(sc);

		CompositeFilter filterProxy = new CompositeFilter();
		filterProxy.setFilters(Arrays.asList(targetFilter));
		filterProxy.init(proxyConfig);

		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		filterProxy.doFilter(request, response, null);

		assertNotNull(targetFilter.filterConfig);
		assertEquals(Boolean.TRUE, request.getAttribute("called"));

		filterProxy.destroy();
		assertNull(targetFilter.filterConfig);
	}


	public static class MockFilter implements Filter {

		public FilterConfig filterConfig;

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
			this.filterConfig = filterConfig;
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) {
			request.setAttribute("called", Boolean.TRUE);
		}

		@Override
		public void destroy() {
			this.filterConfig = null;
		}
	}

}
