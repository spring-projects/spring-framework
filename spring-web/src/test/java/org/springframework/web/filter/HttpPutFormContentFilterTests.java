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

package org.springframework.web.filter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test fixture for {@link HttpPutFormContentFilter}.
 *
 * @author Rossen Stoyanchev
 */
public class HttpPutFormContentFilterTests {

	private HttpPutFormContentFilter filter;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	private MockFilterChain filterChain;

	@Before
	public void setup() {
		filter = new HttpPutFormContentFilter();
		request = new MockHttpServletRequest("PUT", "/");
		request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=ISO-8859-1");
		request.setContentType("application/x-www-form-urlencoded; charset=ISO-8859-1");
		response = new MockHttpServletResponse();
		filterChain = new MockFilterChain();
	}

	@Test
	public void wrapPutAndPatchOnly() throws Exception {
		request.setContent("".getBytes("ISO-8859-1"));
		for (HttpMethod method : HttpMethod.values()) {
			request.setMethod(method.name());
			filterChain = new MockFilterChain();
			filter.doFilter(request, response, filterChain);
			if (method.equals(HttpMethod.PUT) || method.equals(HttpMethod.PATCH)) {
				assertNotSame("Should wrap HTTP method " + method, request, filterChain.getRequest());
			}
			else {
				assertSame("Should not wrap for HTTP method " + method, request, filterChain.getRequest());
			}
		}
	}

	@Test
	public void wrapFormEncodedOnly() throws Exception {
		request.setContent("".getBytes("ISO-8859-1"));
		String[] contentTypes = new String[] {"text/plain", "multipart/form-data"};
		for (String contentType : contentTypes) {
			request.setContentType(contentType);
			filterChain = new MockFilterChain();
			filter.doFilter(request, response, filterChain);
			assertSame("Should not wrap for content type " + contentType, request, filterChain.getRequest());
		}
	}

	@Test
	public void getParameter() throws Exception {
		request.setContent("name=value".getBytes("ISO-8859-1"));
		filter.doFilter(request, response, filterChain);

		assertEquals("value", filterChain.getRequest().getParameter("name"));
	}

	@Test
	public void getParameterFromQueryString() throws Exception {
		request.addParameter("name", "value1");
		request.setContent("name=value2".getBytes("ISO-8859-1"));
		filter.doFilter(request, response, filterChain);

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertEquals("Query string parameters should be listed ahead of form parameters",
				"value1", filterChain.getRequest().getParameter("name"));
	}

	@Test
	public void getParameterNullValue() throws Exception {
		request.setContent("name=value".getBytes("ISO-8859-1"));
		filter.doFilter(request, response, filterChain);

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertNull(filterChain.getRequest().getParameter("noSuchParam"));
	}

	@Test
	public void getParameterNames() throws Exception {
		request.addParameter("name1", "value1");
		request.addParameter("name2", "value2");
		request.setContent("name1=value1&name3=value3&name4=value4".getBytes("ISO-8859-1"));

		filter.doFilter(request, response, filterChain);
		List<String> names = Collections.list(filterChain.getRequest().getParameterNames());

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertEquals(Arrays.asList("name1", "name2", "name3", "name4"), names);
	}

	@Test
	public void getParameterValues() throws Exception {
		request.addParameter("name", "value1");
		request.addParameter("name", "value2");
		request.setContent("name=value3&name=value4".getBytes("ISO-8859-1"));

		filter.doFilter(request, response, filterChain);
		String[] values = filterChain.getRequest().getParameterValues("name");

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertArrayEquals(new String[]{"value1", "value2", "value3", "value4"}, values);
	}

	@Test
	public void getParameterValuesFromQueryString() throws Exception {
		request.addParameter("name", "value1");
		request.addParameter("name", "value2");
		request.setContent("anotherName=anotherValue".getBytes("ISO-8859-1"));

		filter.doFilter(request, response, filterChain);
		String[] values = filterChain.getRequest().getParameterValues("name");

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertArrayEquals(new String[]{"value1", "value2"}, values);
	}

	@Test
	public void getParameterValuesFromFormContent() throws Exception {
		request.addParameter("name", "value1");
		request.addParameter("name", "value2");
		request.setContent("anotherName=anotherValue".getBytes("ISO-8859-1"));

		filter.doFilter(request, response, filterChain);
		String[] values = filterChain.getRequest().getParameterValues("anotherName");

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertArrayEquals(new String[]{"anotherValue"}, values);
	}

	@Test
	public void getParameterValuesInvalidName() throws Exception {
		request.addParameter("name", "value1");
		request.addParameter("name", "value2");
		request.setContent("anotherName=anotherValue".getBytes("ISO-8859-1"));

		filter.doFilter(request, response, filterChain);
		String[] values = filterChain.getRequest().getParameterValues("noSuchParameter");

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertNull(values);
	}

	@Test
	public void getParameterMap() throws Exception {
		request.addParameter("name", "value1");
		request.addParameter("name", "value2");
		request.setContent("name=value3&name4=value4".getBytes("ISO-8859-1"));

		filter.doFilter(request, response, filterChain);
		Map<String, String[]> parameters = filterChain.getRequest().getParameterMap();

		assertNotSame("Request not wrapped", request, filterChain.getRequest());
		assertEquals(2, parameters.size());
		assertArrayEquals(new String[] {"value1", "value2", "value3"}, parameters.get("name"));
		assertArrayEquals(new String[] {"value4"}, parameters.get("name4"));
	}

}
