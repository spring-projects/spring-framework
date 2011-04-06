/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.mvc.method.annotation.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.support.PathVariableMethodArgumentResolver;

/**
 * @author Rossen Stoyanchev
 */
public class PathVariableMethodArgumentResolverTests {

	private PathVariableMethodArgumentResolver resolver;
	
	private MethodParameter pathVarParam;

	private MethodParameter stringParam;

	private MockHttpServletRequest servletRequest;

	private ServletWebRequest webRequest;
	
	@Before
	public void setUp() throws Exception {
		resolver = new PathVariableMethodArgumentResolver(null);
		Method method = getClass().getMethod("handle", String.class, String.class);
		pathVarParam = new MethodParameter(method, 0);
		stringParam = new MethodParameter(method, 1);

		servletRequest = new MockHttpServletRequest();
		MockHttpServletResponse servletResponse = new MockHttpServletResponse();
		webRequest = new ServletWebRequest(servletRequest, servletResponse);
	}
	
	@Test
	public void usesResponseArgument() {
		assertFalse(resolver.usesResponseArgument(null));
	}

	@Test
	public void supportsParameter() {
		assertTrue("Parameter with @PathVariable annotation", resolver.supportsParameter(pathVarParam));
		assertFalse("Parameter without @PathVariable annotation", resolver.supportsParameter(stringParam));
	}

	@Test
	public void resolveStringArgument() throws Exception {
		String expected = "foo";

		Map<String, String> uriTemplateVars = new HashMap<String, String>();
		uriTemplateVars.put("name", expected);
		servletRequest.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		String result = (String) resolver.resolveArgument(pathVarParam, null, webRequest, null);
		assertEquals(expected, result);
	}

	@Test(expected = IllegalStateException.class)
	public void handleMissingValue() throws Exception {
		resolver.resolveArgument(pathVarParam, null, webRequest, null);
		fail("Unresolved path variable should lead to exception.");
	}
	
	public void handle(@PathVariable(value = "name") String param1, String param2) {
	}
	
}
