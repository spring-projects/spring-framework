/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.method.annotation;

import java.lang.reflect.Method;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture with {@link InitBinderDataBinderFactory}.
 *
 * @author Rossen Stoyanchev
 */
class InitBinderDataBinderFactoryTests {

	private final ConfigurableWebBindingInitializer bindingInitializer =
			new ConfigurableWebBindingInitializer();

	private final HandlerMethodArgumentResolverComposite argumentResolvers =
			new HandlerMethodArgumentResolverComposite();

	private final NativeWebRequest webRequest = new ServletWebRequest(new MockHttpServletRequest());


	@Test
	void createBinder() throws Exception {
		WebDataBinderFactory factory = createFactory("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
	}

	@Test
	void createBinderWithGlobalInitialization() throws Exception {
		ConversionService conversionService = new DefaultFormattingConversionService();
		bindingInitializer.setConversionService(conversionService);

		WebDataBinderFactory factory = createFactory("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

		assertThat(dataBinder.getConversionService()).isSameAs(conversionService);
	}

	@Test
	void createBinderWithAttrName() throws Exception {
		WebDataBinderFactory factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, "foo");

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualTo("id");
	}

	@Test
	void createBinderWithAttrNameNoMatch() throws Exception {
		WebDataBinderFactory factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, "invalidName");

		assertThat(dataBinder.getDisallowedFields()).isNull();
	}

	@Test
	void createBinderNullAttrName() throws Exception {
		WebDataBinderFactory factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, null);

		assertThat(dataBinder.getDisallowedFields()).isNull();
	}

	@Test
	void returnValueNotExpected() throws Exception {
		WebDataBinderFactory factory = createFactory("initBinderReturnValue", WebDataBinder.class);
		assertThatIllegalStateException().isThrownBy(() ->
				factory.createBinder(this.webRequest, null, "invalidName"));
	}

	@Test
	void createBinderTypeConversion() throws Exception {
		this.webRequest.getNativeRequest(MockHttpServletRequest.class).setParameter("requestParam", "22");
		this.argumentResolvers.addResolver(new RequestParamMethodArgumentResolver(null, false));

		WebDataBinderFactory factory = createFactory("initBinderTypeConversion", WebDataBinder.class, int.class);
		WebDataBinder dataBinder = factory.createBinder(this.webRequest, null, "foo");

		assertThat(dataBinder.getDisallowedFields()).isNotNull();
		assertThat(dataBinder.getDisallowedFields()[0]).isEqualToIgnoringCase("requestParam-22");
	}

	private WebDataBinderFactory createFactory(String methodName, Class<?>... parameterTypes)
			throws Exception {

		Object handler = new InitBinderHandler();
		Method method = handler.getClass().getMethod(methodName, parameterTypes);

		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(handler, method);
		handlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);
		handlerMethod.setDataBinderFactory(new DefaultDataBinderFactory(null));
		handlerMethod.setParameterNameDiscoverer(new DefaultParameterNameDiscoverer());

		return new InitBinderDataBinderFactory(
				Collections.singletonList(handlerMethod), this.bindingInitializer);
	}


	private static class InitBinderHandler {

		@InitBinder
		public void initBinder(WebDataBinder dataBinder) {
			dataBinder.setDisallowedFields("id");
		}

		@InitBinder(value="foo")
		public void initBinderWithAttributeName(WebDataBinder dataBinder) {
			dataBinder.setDisallowedFields("id");
		}

		@InitBinder
		public String initBinderReturnValue(WebDataBinder dataBinder) {
			return "invalid";
		}

		@InitBinder
		public void initBinderTypeConversion(WebDataBinder dataBinder, @RequestParam int requestParam) {
			dataBinder.setDisallowedFields("requestParam-" + requestParam);
		}
	}

}
