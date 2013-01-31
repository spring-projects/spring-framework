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

package org.springframework.web.method.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.convert.ConversionService;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;

/**
 * Test fixture with {@link InitBinderDataBinderFactory}.
 *
 * @author Rossen Stoyanchev
 */
public class InitBinderDataBinderFactoryTests {

	private ConfigurableWebBindingInitializer bindingInitializer;

	private HandlerMethodArgumentResolverComposite argumentResolvers;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		bindingInitializer = new ConfigurableWebBindingInitializer();
		argumentResolvers = new HandlerMethodArgumentResolverComposite();
		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void createBinder() throws Exception {
		WebDataBinderFactory factory = createBinderFactory("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, null);

		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("id", dataBinder.getDisallowedFields()[0]);
	}

	@Test
	public void createBinderWithGlobalInitialization() throws Exception {
		ConversionService conversionService = new DefaultFormattingConversionService();
		bindingInitializer.setConversionService(conversionService);

		WebDataBinderFactory factory = createBinderFactory("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, null);

		assertSame(conversionService, dataBinder.getConversionService());
	}

	@Test
	public void createBinderWithAttrName() throws Exception {
		WebDataBinderFactory factory = createBinderFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, "foo");

		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("id", dataBinder.getDisallowedFields()[0]);
	}

	@Test
	public void createBinderWithAttrNameNoMatch() throws Exception {
		WebDataBinderFactory factory = createBinderFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, "invalidName");

		assertNull(dataBinder.getDisallowedFields());
	}

	@Test
	public void createBinderNullAttrName() throws Exception {
		WebDataBinderFactory factory = createBinderFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, null);

		assertNull(dataBinder.getDisallowedFields());
	}

	@Test(expected=IllegalStateException.class)
	public void returnValueNotExpected() throws Exception {
		WebDataBinderFactory factory = createBinderFactory("initBinderReturnValue", WebDataBinder.class);
		factory.createBinder(webRequest, null, "invalidName");
	}

	@Test
	public void createBinderTypeConversion() throws Exception {
		webRequest.getNativeRequest(MockHttpServletRequest.class).setParameter("requestParam", "22");
		argumentResolvers.addResolver(new RequestParamMethodArgumentResolver(null, false));

		WebDataBinderFactory factory = createBinderFactory("initBinderTypeConversion", WebDataBinder.class, int.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, "foo");

		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("requestParam-22", dataBinder.getDisallowedFields()[0]);
	}

	private WebDataBinderFactory createBinderFactory(String methodName, Class<?>... parameterTypes)
			throws Exception {

		Object handler = new InitBinderHandler();
		Method method = handler.getClass().getMethod(methodName, parameterTypes);

		InvocableHandlerMethod handlerMethod = new InvocableHandlerMethod(handler, method);
		handlerMethod.setHandlerMethodArgumentResolvers(argumentResolvers);
		handlerMethod.setDataBinderFactory(new DefaultDataBinderFactory(null));
		handlerMethod.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());

		return new InitBinderDataBinderFactory(Arrays.asList(handlerMethod), bindingInitializer);
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
