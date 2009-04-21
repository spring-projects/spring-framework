/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.annotation.RequestMethod;

/** @author Arjen Poutsma */
public class ServletAnnotationMappingUtilsTests {

	@Test
	public void checkRequestMethodMatch() {
		RequestMethod[] methods = new RequestMethod[]{RequestMethod.GET, RequestMethod.POST};
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		boolean result = ServletAnnotationMappingUtils.checkRequestMethod(methods, request);
		assertTrue("Invalid request method result", result);
	}

	@Test
	public void checkRequestMethodNoMatch() {
		RequestMethod[] methods = new RequestMethod[]{RequestMethod.GET, RequestMethod.POST};
		MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/");
		boolean result = ServletAnnotationMappingUtils.checkRequestMethod(methods, request);
		assertFalse("Invalid request method result", result);
	}

	@Test
	public void checkParametersSimpleMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addParameter("param1", "value1");
		String[] params = new String[]{"param1", "!param2"};
		boolean result = ServletAnnotationMappingUtils.checkParameters(params, request);
		assertTrue("Invalid request method result", result);
	}

	@Test
	public void checkParametersSimpleNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addParameter("param1", "value1");
		request.addParameter("param2", "value2");
		String[] params = new String[]{"param1", "!param2"};
		boolean result = ServletAnnotationMappingUtils.checkParameters(params, request);
		assertFalse("Invalid request method result", result);
	}

	@Test
	public void checkParametersKeyValueMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addParameter("param1", "value1");
		String[] params = new String[]{"param1=value1"};
		boolean result = ServletAnnotationMappingUtils.checkParameters(params, request);
		assertTrue("Invalid request method result", result);
	}
	
	@Test
	public void checkParametersKeyValueNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addParameter("param1", "value1");
		String[] params = new String[]{"param1=foo"};
		boolean result = ServletAnnotationMappingUtils.checkParameters(params, request);
		assertFalse("Invalid request method result", result);
	}

	@Test
	public void checkHeadersSimpleMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("header1", "value1");
		String[] headers = new String[]{"header1", "!header2"};
		boolean result = ServletAnnotationMappingUtils.checkHeaders(headers, request);
		assertTrue("Invalid request method result", result);
	}

	@Test
	public void checkHeadersSimpleNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("header1", "value1");
		request.addHeader("header2", "value2");
		String[] headers = new String[]{"header1", "!header2"};
		boolean result = ServletAnnotationMappingUtils.checkHeaders(headers, request);
		assertFalse("Invalid request method result", result);
	}

	@Test
	public void checkHeadersKeyValueMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("header1", "value1");
		String[] headers = new String[]{"header1=value1"};
		boolean result = ServletAnnotationMappingUtils.checkHeaders(headers, request);
		assertTrue("Invalid request method result", result);
	}

	@Test
	public void checkHeadersKeyValueNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("header1", "value1");
		String[] headers = new String[]{"header1=foo"};
		boolean result = ServletAnnotationMappingUtils.checkHeaders(headers, request);
		assertFalse("Invalid request method result", result);
	}

	@Test
	public void checkHeadersAcceptMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("Accept", "application/pdf, text/html");
		String[] headers = new String[]{"accept=text/html, application/*"};
		boolean result = ServletAnnotationMappingUtils.checkHeaders(headers, request);
		assertTrue("Invalid request method result", result);
	}

	@Test
	public void checkHeadersAcceptNoMatch() {
		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/");
		request.addHeader("Accept", "application/pdf, text/html");
		String[] headers = new String[]{"accept=audio/basic, application/xml"};
		boolean result = ServletAnnotationMappingUtils.checkHeaders(headers, request);
		assertFalse("Invalid request method result", result);
	}

}
