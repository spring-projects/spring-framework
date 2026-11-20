/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link PathVariableMapMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class PathVariableMapMethodArgumentResolverTests {

	private PathVariableMapMethodArgumentResolver resolver;

	private ModelAndViewContainer mavContainer;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest request;

	private MethodParameter paramMap;
	private MethodParameter paramNamedMap;
	private MethodParameter paramMapNoAnnot;


	@BeforeEach
	void setup() throws Exception {
		resolver = new PathVariableMapMethodArgumentResolver();
		mavContainer = new ModelAndViewContainer();
		request = new MockHttpServletRequest();
		webRequest = new ServletWebRequest(request, new MockHttpServletResponse());

		Method method = getClass().getMethod("handle", Map.class, Map.class, Map.class);
		paramMap = new MethodParameter(method, 0);
		paramNamedMap = new MethodParameter(method, 1);
		paramMapNoAnnot = new MethodParameter(method, 2);
	}


	@Test
	void supportsParameter() {
		assertThat(resolver.supportsParameter(paramMap)).isTrue();
		assertThat(resolver.supportsParameter(paramNamedMap)).isFalse();
		assertThat(resolver.supportsParameter(paramMapNoAnnot)).isFalse();
	}

	@Test
	void resolveArgument() throws Exception {
		Map<String, String> uriTemplateVars = new HashMap<>();
		uriTemplateVars.put("name1", "value1");
		uriTemplateVars.put("name2", "value2");
		request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, uriTemplateVars);

		Object result = resolver.resolveArgument(paramMap, mavContainer, webRequest, null);

		assertThat(result).isEqualTo(uriTemplateVars);
	}

	@Test
	@SuppressWarnings("unchecked")
	void resolveArgumentNoUriVars() throws Exception {
		Map<String, String> map = (Map<String, String>) resolver.resolveArgument(paramMap, mavContainer, webRequest, null);

		assertThat(map).isEqualTo(Collections.emptyMap());
	}


	public void handle(
			@PathVariable Map<String, String> map,
			@PathVariable(value = "name") Map<String, String> namedMap,
			Map<String, String> mapWithoutAnnotat) {
	}

}
