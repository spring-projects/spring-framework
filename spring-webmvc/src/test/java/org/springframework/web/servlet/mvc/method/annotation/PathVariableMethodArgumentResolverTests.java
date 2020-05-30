/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
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
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test fixture with {@link PathVariableMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 */
public class PathVariableMethodArgumentResolverTests {

	private PathVariableMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	private MethodParameter paramNamedString;
	private MethodParameter paramString;
	private MethodParameter paramNotRequired;
	private MethodParameter paramOptional;


	@BeforeEach
	public void setup() throws Exception {
		resolver = new PathVariableMethodArgumentResolver();
		mavContainer = new ModelAndViewContainer();
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

		Method method = ReflectionUtils.findMethod(getClass(), "handle", (Class<?>[]) null);
		paramNamedString = new SynthesizingMethodParameter(method, 0);
		paramString = new SynthesizingMethodParameter(method, 1);
		paramNotRequired = new SynthesizingMethodParameter(method, 2);
		paramOptional = new SynthesizingMethodParameter(method, 3);
	}


	@Test
	public void supportsParameter() {
		assertThat(resolver.supportsParameter(paramNamedString)).as("Parameter with @PathVariable annotation").isTrue();
		assertThat(resolver.supportsParameter(paramString)).as("Parameter without @PathVariable annotation").isFalse();
	}

	@Test
	public void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		String result = (String) resolver.resolveArgument(paramNamedString, mavContainer, webRequest, null);
		assertThat(result).as("PathVariable not resolved correctly").isEqualTo("value");

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertThat(pathVars).isNotNull();
		assertThat(pathVars.size()).isEqualTo(1);
		assertThat(pathVars.get("name")).isEqualTo("value");
	}

	@Test
	public void resolveArgumentNotRequired() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		String result = (String) resolver.resolveArgument(paramNotRequired, mavContainer, webRequest, null);
		assertThat(result).as("PathVariable not resolved correctly").isEqualTo("value");

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertThat(pathVars).isNotNull();
		assertThat(pathVars.size()).isEqualTo(1);
		assertThat(pathVars.get("name")).isEqualTo("value");
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
		assertThat(result.get()).as("PathVariable not resolved correctly").isEqualTo("value");

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertThat(pathVars).isNotNull();
		assertThat(pathVars.size()).isEqualTo(1);
		assertThat(pathVars.get("name")).isEqualTo(Optional.of("value"));
	}

	@Test
	public void resolveArgumentWithExistingPathVars() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name", "value");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		uriTemplateVars.put("oldName", "oldValue");
		request.setAttribute(View.PATH_VARIABLES, uriTemplateVars);

		String result = (String) resolver.resolveArgument(paramNamedString, mavContainer, webRequest, null);
		assertThat(result).as("PathVariable not resolved correctly").isEqualTo("value");

		@SuppressWarnings("unchecked")
		Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		assertThat(pathVars).isNotNull();
		assertThat(pathVars.size()).isEqualTo(2);
		assertThat(pathVars.get("name")).isEqualTo("value");
		assertThat(pathVars.get("oldName")).isEqualTo("oldValue");
	}

	@Test
	public void handleMissingValue() throws Exception {
		assertThatExceptionOfType(MissingPathVariableException.class).isThrownBy(() ->
				resolver.resolveArgument(paramNamedString, mavContainer, webRequest, null));
	}

	@Test
	public void nullIfNotRequired() throws Exception {
		assertThat(resolver.resolveArgument(paramNotRequired, mavContainer, webRequest, null)).isNull();
	}

	@Test
	public void wrapEmptyWithOptional() throws Exception {
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setConversionService(new DefaultConversionService());
		WebDataBinderFactory binderFactory = new DefaultDataBinderFactory(initializer);

		assertThat(resolver.resolveArgument(paramOptional, mavContainer, webRequest, binderFactory)).isEqualTo(Optional.empty());
	}


	@SuppressWarnings("unused")
	public void handle(@PathVariable("name") String param1, String param2,
			@PathVariable(name="name", required = false) String param3,
			@PathVariable("name") Optional<String> param4) {
	}

}
