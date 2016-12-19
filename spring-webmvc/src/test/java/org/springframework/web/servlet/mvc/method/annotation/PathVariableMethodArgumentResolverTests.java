/*
 * Copyright 2002-2016 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.View;

import static org.junit.Assert.*;

/**
 * Test fixture with {@link PathVariableMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class PathVariableMethodArgumentResolverTests {

	private PathVariableMethodArgumentResolver resolver;

	private MethodParameter paramNamedString;

	private MethodParameter paramString;

	private MethodParameter paramNotRequired;

	private MethodParameter paramOptional;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;


	@Before
	public void setUp() throws Exception {
		resolver = new PathVariableMethodArgumentResolver();

		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		paramNamedString = new SynthesizingMethodParameter(method, 0);
		paramString = new SynthesizingMethodParameter(method, 1);
		paramNotRequired = new SynthesizingMethodParameter(method, 2);
		paramOptional = new SynthesizingMethodParameter(method, 3);

		mavContainer = new ModelAndViewContainer();
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());
	}


	@Test
	public void supportsParameter() {
		assertTrue("Parameter with @PathVariable annotation", resolver.supportsParameter(paramNamedString));
		assertFalse("Parameter without @PathVariable annotation", resolver.supportsParameter(paramString));
	}

	@Test
	public void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		String result = (String) resolver.resolveArgument(paramNamedString, mavContainer, webRequest, null);
		assertEquals("PathVariable not resolved correctly", "value", result);

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertNotNull(pathVars);
		assertEquals(1, pathVars.size());
		assertEquals("value", pathVars.get("name"));
	}

	@Test
	public void resolveArgumentNotRequired() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		String result = (String) resolver.resolveArgument(paramNotRequired, mavContainer, webRequest, null);
		assertEquals("PathVariable not resolved correctly", "value", result);

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertNotNull(pathVars);
		assertEquals(1, pathVars.size());
		assertEquals("value", pathVars.get("name"));
	}

	@Test
	public void resolveArgumentWrappedAsOptional() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		@SuppressWarnings("unchecked")
		Optional<String> result = (Optional<String>)
				resolver.resolveArgument(paramOptional, mavContainer, webRequest, binderFactory);
		assertEquals("PathVariable not resolved correctly", "value", result.get());

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertNotNull(pathVars);
		assertEquals(1, pathVars.size());
		assertEquals(Optional.of("value"), pathVars.get("name"));
	}

	@Test
	public void resolveArgumentWithExistingPathVars() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		uriTemplateVars.put("oldName", "oldValue");
		request.setAttribute(View.PATH_VARIABLES, uriTemplateVars);

		String result = (String) resolver.resolveArgument(paramNamedString, mavContainer, webRequest, null);
		assertEquals("PathVariable not resolved correctly", "value", result);

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertNotNull(pathVars);
		assertEquals(2, pathVars.size());
		assertEquals("value", pathVars.get("name"));
		assertEquals("oldValue", pathVars.get("oldName"));
	}

	@Test(expected = MissingPathVariableException.class)
	public void handleMissingValue() throws Exception {
		resolver.resolveArgument(paramNamedString, mavContainer, webRequest, null);
		fail("Unresolved path variable should lead to exception");
	}

	@Test
	public void nullIfNotRequired() throws Exception {
		assertNull(resolver.resolveArgument(paramNotRequired, mavContainer, webRequest, null));
	}

	@Test
	public void wrapEmptyWithOptional() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		assertEquals(Optional.empty(), resolver.resolveArgument(paramOptional, mavContainer, webRequest, binderFactory));
	}


	@SuppressWarnings("unused")
	public void handle(@PathVariable("name") String param1, String param2,
			@PathVariable(name="name", required = false) String param3,
			@PathVariable("name") Optional<String> param4) {
	}

}
