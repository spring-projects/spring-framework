/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.mock.web;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

/**
 * @author Rick Evans
 * @author Mark Fisher
 */
public class MockHttpServletRequestTests extends TestCase {

	public void testHttpHeaderNameCasingIsPreserved() throws Exception {
		String headerName = "Header1";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(headerName, "value1");
		Enumeration requestHeaders = request.getHeaderNames();
		assertNotNull(requestHeaders);
		assertEquals("HTTP header casing not being preserved", headerName, requestHeaders.nextElement());
	}

	public void testSetMultipleParameters() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("key1", "value1");
		request.setParameter("key2", "value2");
		Map params = new HashMap(2);
		params.put("key1", "newValue1");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.setParameters(params);
		String[] values1 = request.getParameterValues("key1");
		assertEquals(1, values1.length);
		assertEquals("newValue1", request.getParameter("key1"));
		assertEquals("value2", request.getParameter("key2"));
		String[] values3 = request.getParameterValues("key3");
		assertEquals(2, values3.length);
		assertEquals("value3A", values3[0]);
		assertEquals("value3B", values3[1]);
	}

	public void testAddMultipleParameters() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("key1", "value1");
		request.setParameter("key2", "value2");
		Map params = new HashMap(2);
		params.put("key1", "newValue1");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.addParameters(params);
		String[] values1 = request.getParameterValues("key1");
		assertEquals(2, values1.length);
		assertEquals("value1", values1[0]);
		assertEquals("newValue1", values1[1]);
		assertEquals("value2", request.getParameter("key2"));
		String[] values3 = request.getParameterValues("key3");
		assertEquals(2, values3.length);
		assertEquals("value3A", values3[0]);
		assertEquals("value3B", values3[1]);
	}

	public void testRemoveAllParameters() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("key1", "value1");
		Map params = new HashMap(2);
		params.put("key2", "value2");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.addParameters(params);
		assertEquals(3, request.getParameterMap().size());
		request.removeAllParameters();
		assertEquals(0, request.getParameterMap().size());
	}

}
