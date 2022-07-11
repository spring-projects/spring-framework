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
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Base class for {@code @RequestAttribute} and {@code @SessionAttribute} method
 * argument resolution tests.
 *
 * @author Rossen Stoyanchev
 * @since 4.3
 */
public abstract class AbstractRequestAttributesArgumentResolverTests {

	private ServletWebRequest webRequest;

	private HandlerMethodArgumentResolver resolver;

	private Method handleMethod;


	@BeforeEach
	public void setup() throws Exception {
		HttpServletRequest request = new MockHttpServletRequest();
		HttpServletResponse response = new MockHttpServletResponse();
		this.webRequest = new ServletWebRequest(request, response);

		this.resolver = createResolver();

		this.handleMethod = AbstractRequestAttributesArgumentResolverTests.class
				.getDeclaredMethod(getHandleMethodName(), Foo.class, Foo.class, Foo.class, Optional.class);
	}


	protected abstract HandlerMethodArgumentResolver createResolver();

	protected abstract String getHandleMethodName();

	protected abstract int getScope();


	@Test
	public void supportsParameter() throws Exception {
		assertThat(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, 0))).isTrue();
		assertThat(this.resolver.supportsParameter(new MethodParameter(this.handleMethod, -1))).isFalse();
	}

	@Test
	public void resolve() throws Exception {
		MethodParameter param = initMethodParameter(0);
		assertThatExceptionOfType(ServletRequestBindingException.class).isThrownBy(() ->
				testResolveArgument(param))
			.withMessageStartingWith("Missing ");

		Foo foo = new Foo();
		this.webRequest.setAttribute("foo", foo, getScope());
		assertThat(testResolveArgument(param)).isSameAs(foo);
	}

	@Test
	public void resolveWithName() throws Exception {
		MethodParameter param = initMethodParameter(1);
		Foo foo = new Foo();
		this.webRequest.setAttribute("specialFoo", foo, getScope());
		assertThat(testResolveArgument(param)).isSameAs(foo);
	}

	@Test
	public void resolveNotRequired() throws Exception {
		MethodParameter param = initMethodParameter(2);
		assertThat(testResolveArgument(param)).isNull();

		Foo foo = new Foo();
		this.webRequest.setAttribute("foo", foo, getScope());
		assertThat(testResolveArgument(param)).isSameAs(foo);
	}

	@Test
	public void resolveOptional() throws Exception {
		WebDataBinder dataBinder = new WebRequestDataBinder(null);
		dataBinder.setConversionService(new DefaultConversionService());
		WebDataBinderFactory factory = mock(WebDataBinderFactory.class);
		given(factory.createBinder(this.webRequest, null, "foo")).willReturn(dataBinder);

		MethodParameter param = initMethodParameter(3);
		Object actual = testResolveArgument(param, factory);
		assertThat(actual).isNotNull();
		assertThat(actual.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional<?>) actual).isPresent()).isFalse();

		Foo foo = new Foo();
		this.webRequest.setAttribute("foo", foo, getScope());

		actual = testResolveArgument(param, factory);
		assertThat(actual).isNotNull();
		assertThat(actual.getClass()).isEqualTo(Optional.class);
		assertThat(((Optional<?>) actual).isPresent()).isTrue();
		assertThat(((Optional<?>) actual).get()).isSameAs(foo);
	}

	private Object testResolveArgument(MethodParameter param) throws Exception {
		return testResolveArgument(param, null);
	}

	private Object testResolveArgument(MethodParameter param, WebDataBinderFactory factory) throws Exception {
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		return this.resolver.resolveArgument(param, mavContainer, this.webRequest, factory);
	}

	private MethodParameter initMethodParameter(int parameterIndex) {
		MethodParameter param = new SynthesizingMethodParameter(this.handleMethod, parameterIndex);
		param.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
		return param.withContainingClass(this.resolver.getClass());
	}


	@SuppressWarnings("unused")
	private void handleWithRequestAttribute(
			@RequestAttribute Foo foo,
			@RequestAttribute("specialFoo") Foo namedFoo,
			@RequestAttribute(name="foo", required = false) Foo notRequiredFoo,
			@RequestAttribute(name="foo") Optional<Foo> optionalFoo) {
	}

	@SuppressWarnings("unused")
	private void handleWithSessionAttribute(
			@SessionAttribute Foo foo,
			@SessionAttribute("specialFoo") Foo namedFoo,
			@SessionAttribute(name="foo", required = false) Foo notRequiredFoo,
			@SessionAttribute(name="foo") Optional<Foo> optionalFoo) {
	}


	private static class Foo {
	}

}
