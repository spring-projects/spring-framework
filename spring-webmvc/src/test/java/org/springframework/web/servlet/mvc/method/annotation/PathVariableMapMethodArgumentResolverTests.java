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

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link PathVariableMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(Parameterized.class)
public class PathVariableMapMethodArgumentResolverTests {

	@Parameters(name = "{0}")
	public static List<Object[]> createParameters() {
		List<Object[]> parameters = new ArrayList<Object[]>();
		parameters.add(new Object[] { Foo.class });
		parameters.add(new Object[] { Bar.class });
		parameters.add(new Object[] { Baz.class });
		return parameters;
	}

	@Parameter(0)
	public Class<?> target;

	private PathVariableMapMethodArgumentResolver resolver;

	private MethodParameter paramMap;

	private MethodParameter paramNamedMap;

	private MethodParameter paramMapNoAnnot;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	@Before
	public void setUp() throws Exception {
		resolver = new PathVariableMapMethodArgumentResolver();

		Method method = target.getMethod("handle", Map.class, Map.class, Map.class);
		paramMap = new MethodParameter(method, 0);
		paramNamedMap = new MethodParameter(method, 1);
		paramMapNoAnnot = new MethodParameter(method, 2);

		mavContainer = new ModelAndViewContainer();
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}

	@Test
	public void supportsParameter() {
		assertTrue(resolver.supportsParameter(paramMap));
		assertFalse(resolver.supportsParameter(paramNamedMap));
		assertFalse(resolver.supportsParameter(paramMapNoAnnot));
	}

	@Test
	public void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<String, String>();
		uriTemplateVars.put("name1", "value1");
		uriTemplateVars.put("name2", "value2");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		Object result = resolver.resolveArgument(paramMap, mavContainer, webRequest, null);

		assertEquals(uriTemplateVars, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void resolveArgumentNoUriVars() throws Exception {
		Map<String, String> map = (Map<String, String>) resolver.resolveArgument(paramMap, mavContainer, webRequest, null);

		assertEquals(Collections.emptyMap(), map);
	}


	public interface Foo {
		void handle(
			@PathVariable Map<String, String> map,
			@PathVariable(value = "name") Map<String, String> namedMap,
			Map<String, String> mapWithoutAnnotat);
	}

	public static abstract class Bar implements Foo {
	}

	public class Baz extends Bar {

		@Override
		public void handle(Map<String, String> map, Map<String, String> namedMap,
				Map<String, String> mapWithoutAnnotat) {
		}

	}
}
