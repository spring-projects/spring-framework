/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.mvc.method.annotation;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test fixture with {@link UriComponentsBuilderMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class UriComponentsBuilderMethodArgumentResolverTests {

	private UriComponentsBuilderMethodArgumentResolver resolver;

	private ServletWebRequest webRequest;

	private MockHttpServletRequest servletRequest;

	private MethodParameter builderParam;
	private MethodParameter servletBuilderParam;
	private MethodParameter intParam;


	@BeforeEach
	void setup() throws Exception {
		this.resolver = new UriComponentsBuilderMethodArgumentResolver();
		this.servletRequest = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(this.servletRequest);

		Method method = getClass().getDeclaredMethod(
				"handle", UriComponentsBuilder.class, ServletUriComponentsBuilder.class, int.class);
		this.builderParam = new MethodParameter(method, 0);
		this.servletBuilderParam = new MethodParameter(method, 1);
		this.intParam = new MethodParameter(method, 2);
	}


	@Test
	void supportsParameter() throws Exception {
		assertThat(this.resolver.supportsParameter(this.builderParam)).isTrue();
		assertThat(this.resolver.supportsParameter(this.servletBuilderParam)).isTrue();
		assertThat(this.resolver.supportsParameter(this.intParam)).isFalse();
	}

	@Test
	void resolveArgument() throws Exception {
		this.servletRequest.setContextPath("/myapp");
		this.servletRequest.setServletPath("/main");
		this.servletRequest.setPathInfo("/accounts");

		Object actual = this.resolver.resolveArgument(this.builderParam, new ModelAndViewContainer(), this.webRequest, null);

		assertThat(actual).isNotNull();
		assertThat(actual.getClass()).isEqualTo(ServletUriComponentsBuilder.class);
		assertThat(((ServletUriComponentsBuilder) actual).build().toUriString()).isEqualTo("http://localhost/myapp/main");
	}


	void handle(UriComponentsBuilder builder, ServletUriComponentsBuilder servletBuilder, int value) {
	}

}
