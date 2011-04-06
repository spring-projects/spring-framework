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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;
import org.springframework.web.bind.support.DefaultDataBinderFactory;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.annotation.support.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;

/**
 * Test fixture for {@link InitBinderMethodDataBinderFactory} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class InitBinderMethodDataBinderFactoryTests {

	private MockHttpServletRequest request;

	private NativeWebRequest webRequest;
	
	private ConfigurableWebBindingInitializer bindingInitializer;

	@Before
	public void setUp() throws Exception {
		this.request = new MockHttpServletRequest();
		this.webRequest = new ServletWebRequest(request);
		this.bindingInitializer = new ConfigurableWebBindingInitializer();
	}
	
	@Test
	public void createBinder() throws Exception {
		InitBinderMethodDataBinderFactory factory = createFactory("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, null);
		
		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("id", dataBinder.getDisallowedFields()[0]);
	}

	@Test
	public void createBinderWithGlobalInitialization() throws Exception {
		ConversionService conversionService = new DefaultFormattingConversionService();
		bindingInitializer.setConversionService(conversionService );
		
		InitBinderMethodDataBinderFactory factory = createFactory("initBinder", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, null);
		
		assertSame(conversionService, dataBinder.getConversionService());
	}

	@Test
	public void createBinderWithAttrName() throws Exception {
		InitBinderMethodDataBinderFactory factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, "foo");
		
		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("id", dataBinder.getDisallowedFields()[0]);
	}

	@Test
	public void createBinderWithAttrNameNoMatch() throws Exception {
		WebDataBinderFactory factory = createFactory("initBinderWithAttributeName", WebDataBinder.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, "invalidName");
		
		assertNull(dataBinder.getDisallowedFields());
	}
	
	@Test(expected=IllegalStateException.class)
	public void returnValueNotExpected() throws Exception {
		WebDataBinderFactory factory = createFactory("initBinderReturnValue", WebDataBinder.class);
		factory.createBinder(webRequest, null, "invalidName");
	}

	@Test
	public void createBinderTypeConversion() throws Exception {
		request.setParameter("requestParam", "22");

		HandlerMethodArgumentResolverComposite argResolvers = new HandlerMethodArgumentResolverComposite();
		argResolvers.registerArgumentResolver(new RequestParamMethodArgumentResolver(null, false));

		String methodName = "initBinderTypeConversion";
		WebDataBinderFactory factory = createFactory(argResolvers, methodName, WebDataBinder.class, int.class);
		WebDataBinder dataBinder = factory.createBinder(webRequest, null, "foo");

		assertNotNull(dataBinder.getDisallowedFields());
		assertEquals("requestParam-22", dataBinder.getDisallowedFields()[0]);
	}

	private InitBinderMethodDataBinderFactory createFactory(String methodName, Class<?>... parameterTypes)
			throws Exception {
		return createFactory(new HandlerMethodArgumentResolverComposite(), methodName, parameterTypes);
	}
	
	private InitBinderMethodDataBinderFactory createFactory(HandlerMethodArgumentResolverComposite argResolvers,
			String methodName, Class<?>... parameterTypes) throws Exception {
		Object handler = new InitBinderHandler();
		Method method = InitBinderHandler.class.getMethod(methodName, parameterTypes);

		InvocableHandlerMethod controllerMethod = new InvocableHandlerMethod(handler, method);
		controllerMethod.setHandlerMethodArgumentResolvers(argResolvers);
		controllerMethod.setDataBinderFactory(new DefaultDataBinderFactory(null));
		controllerMethod.setParameterNameDiscoverer(new LocalVariableTableParameterNameDiscoverer());
		
		return new InitBinderMethodDataBinderFactory(Arrays.asList(controllerMethod), bindingInitializer);
	}	

	private static class InitBinderHandler {

		@SuppressWarnings("unused")
		@InitBinder
		public void initBinder(WebDataBinder dataBinder) {
			dataBinder.setDisallowedFields("id");
		}
		
		@SuppressWarnings("unused")
		@InitBinder(value="foo")
		public void initBinderWithAttributeName(WebDataBinder dataBinder) {
			dataBinder.setDisallowedFields("id");
		}
		
		@SuppressWarnings("unused")
		@InitBinder
		public String initBinderReturnValue(WebDataBinder dataBinder) {
			return "invalid";
		}
		
		@SuppressWarnings("unused")
		@InitBinder
		public void initBinderTypeConversion(WebDataBinder dataBinder, @RequestParam int requestParam) {
			dataBinder.setDisallowedFields("requestParam-" + requestParam);
		}
	}
	
}
