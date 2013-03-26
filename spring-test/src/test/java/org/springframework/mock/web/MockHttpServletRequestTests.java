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

package org.springframework.mock.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link MockHttpServletRequest}.
 *
 * @author Rick Evans
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
public class MockHttpServletRequestTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();


	@Test
	public void setContentType() {
		String contentType = "test/plain";
		request.setContentType(contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader("Content-Type"));
		assertNull(request.getCharacterEncoding());
	}

	@Test
	public void setContentTypeUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		request.setContentType(contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader("Content-Type"));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void contentTypeHeader() {
		String contentType = "test/plain";
		request.addHeader("Content-Type", contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader("Content-Type"));
		assertNull(request.getCharacterEncoding());
	}

	@Test
	public void contentTypeHeaderUTF8() {
		String contentType = "test/plain;charset=UTF-8";
		request.addHeader("Content-Type", contentType);
		assertEquals(contentType, request.getContentType());
		assertEquals(contentType, request.getHeader("Content-Type"));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void setContentTypeThenCharacterEncoding() {
		request.setContentType("test/plain");
		request.setCharacterEncoding("UTF-8");
		assertEquals("test/plain", request.getContentType());
		assertEquals("test/plain;charset=UTF-8", request.getHeader("Content-Type"));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void setCharacterEncodingThenContentType() {
		request.setCharacterEncoding("UTF-8");
		request.setContentType("test/plain");
		assertEquals("test/plain", request.getContentType());
		assertEquals("test/plain;charset=UTF-8", request.getHeader("Content-Type"));
		assertEquals("UTF-8", request.getCharacterEncoding());
	}

	@Test
	public void httpHeaderNameCasingIsPreserved() throws Exception {
		String headerName = "Header1";
		request.addHeader(headerName, "value1");
		Enumeration<String> requestHeaders = request.getHeaderNames();
		assertNotNull(requestHeaders);
		assertEquals("HTTP header casing not being preserved", headerName, requestHeaders.nextElement());
	}

	@Test
	public void nullParameterName() {
		assertNull(request.getParameter(null));
		assertNull(request.getParameterValues(null));
	}

	@Test
	public void setMultipleParameters() {
		request.setParameter("key1", "value1");
		request.setParameter("key2", "value2");
		Map<String, Object> params = new HashMap<String, Object>(2);
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

	@Test
	public void addMultipleParameters() {
		request.setParameter("key1", "value1");
		request.setParameter("key2", "value2");
		Map<String, Object> params = new HashMap<String, Object>(2);
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

	@Test
	public void removeAllParameters() {
		request.setParameter("key1", "value1");
		Map<String, Object> params = new HashMap<String, Object>(2);
		params.put("key2", "value2");
		params.put("key3", new String[] { "value3A", "value3B" });
		request.addParameters(params);
		assertEquals(3, request.getParameterMap().size());
		request.removeAllParameters();
		assertEquals(0, request.getParameterMap().size());
	}

	@Test
	public void defaultLocale() {
		Locale originalDefaultLocale = Locale.getDefault();
		try {
			Locale newDefaultLocale = originalDefaultLocale.equals(Locale.GERMANY) ? Locale.FRANCE : Locale.GERMANY;
			Locale.setDefault(newDefaultLocale);
			// Create the request after changing the default locale.
			MockHttpServletRequest request = new MockHttpServletRequest();
			assertFalse(newDefaultLocale.equals(request.getLocale()));
			assertEquals(Locale.ENGLISH, request.getLocale());
		}
		finally {
			Locale.setDefault(originalDefaultLocale);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPreferredLocalesWithNullList() {
		request.setPreferredLocales(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void setPreferredLocalesWithEmptyList() {
		request.setPreferredLocales(new ArrayList<Locale>());
	}

	@Test
	public void setPreferredLocales() {
		List<Locale> preferredLocales = Arrays.asList(Locale.ITALY, Locale.CHINA);
		request.setPreferredLocales(preferredLocales);
		assertEqualEnumerations(Collections.enumeration(preferredLocales), request.getLocales());
	}

	private void assertEqualEnumerations(Enumeration<?> enum1, Enumeration<?> enum2) {
		assertNotNull(enum1);
		assertNotNull(enum2);
		int count = 0;
		while (enum1.hasMoreElements()) {
			assertTrue("enumerations must be equal in length", enum2.hasMoreElements());
			assertEquals("enumeration element #" + ++count, enum1.nextElement(), enum2.nextElement());
		}
	}

}
