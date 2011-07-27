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

package org.springframework.web.servlet.mvc.method.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.annotation.support.ModelMethodProcessor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolverComposite;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.method.support.InvocableHandlerMethod;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.support.ServletRequestMethodArgumentResolver;

/**
 * Fine-grained {@link RequestMappingHandlerAdapter} unit tests.
 *
 * <p>For higher-level adapter tests see:
 * <ul>
 * <li>{@link ServletAnnotationControllerHandlerMethodTests}
 * <li>{@link HandlerMethodAnnotationDetectionTests}
 * <li>{@link RequestMappingHandlerAdapterIntegrationTests}
 * </ul>
 *
 * @author Rossen Stoyanchev
 */
public class RequestMappingHandlerAdapterTests {

	private RequestMappingHandlerAdapter handlerAdapter;
	
	private MockHttpServletRequest request;
	
	private MockHttpServletResponse response;

	@Before
	public void setup() throws Exception {
		this.handlerAdapter = new RequestMappingHandlerAdapter();
		this.handlerAdapter.setApplicationContext(new GenericWebApplicationContext());

		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	public void cacheControlWithoutSessionAttributes() throws Exception {
		handlerAdapter.afterPropertiesSet();
		handlerAdapter.setCacheSeconds(100);
		handlerAdapter.handle(request, response, handlerMethod(new SimpleHandler(), "handle"));

		assertTrue(response.getHeader("Cache-Control").toString().contains("max-age"));
	}

	@Test
	public void cacheControlWithSessionAttributes() throws Exception {
		handlerAdapter.afterPropertiesSet();
		handlerAdapter.setCacheSeconds(100);
		handlerAdapter.handle(request, response, handlerMethod(new SessionAttributeHandler(), "handle"));

		assertEquals("no-cache", response.getHeader("Cache-Control"));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void setArgumentResolvers() {
		List<HandlerMethodArgumentResolver> expected  = new ArrayList<HandlerMethodArgumentResolver>();
		expected.add(new ServletRequestMethodArgumentResolver());
		handlerAdapter.setArgumentResolvers(expected);
		handlerAdapter.afterPropertiesSet();
		
		HandlerMethodArgumentResolverComposite composite = (HandlerMethodArgumentResolverComposite) 
			new DirectFieldAccessor(handlerAdapter).getPropertyValue("argumentResolvers");

		List<HandlerMethodArgumentResolver> actual = (List<HandlerMethodArgumentResolver>)
			new DirectFieldAccessor(composite).getPropertyValue("argumentResolvers");
		
		assertEquals(expected, actual);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void setInitBinderArgumentResolvers() {
		List<HandlerMethodArgumentResolver> expected  = new ArrayList<HandlerMethodArgumentResolver>();
		expected.add(new ServletRequestMethodArgumentResolver());
		handlerAdapter.setInitBinderArgumentResolvers(expected);
		handlerAdapter.afterPropertiesSet();
		
		HandlerMethodArgumentResolverComposite composite = (HandlerMethodArgumentResolverComposite) 
			new DirectFieldAccessor(handlerAdapter).getPropertyValue("initBinderArgumentResolvers");

		List<HandlerMethodArgumentResolver> actual = (List<HandlerMethodArgumentResolver>)
			new DirectFieldAccessor(composite).getPropertyValue("argumentResolvers");
		
		assertEquals(expected, actual);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void setReturnValueHandlers() {
		List<HandlerMethodReturnValueHandler> expected  = new ArrayList<HandlerMethodReturnValueHandler>();
		expected.add(new ModelMethodProcessor());
		handlerAdapter.setReturnValueHandlers(expected);
		handlerAdapter.afterPropertiesSet();
		
		HandlerMethodReturnValueHandlerComposite composite = (HandlerMethodReturnValueHandlerComposite) 
			new DirectFieldAccessor(handlerAdapter).getPropertyValue("returnValueHandlers");

		List<HandlerMethodReturnValueHandler> actual = (List<HandlerMethodReturnValueHandler>)
			new DirectFieldAccessor(composite).getPropertyValue("returnValueHandlers");
		
		assertEquals(expected, actual);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void setCustomArgumentResolvers() {
		TestHanderMethodArgumentResolver resolver = new TestHanderMethodArgumentResolver();
		handlerAdapter.setCustomArgumentResolvers(Arrays.<HandlerMethodArgumentResolver>asList(resolver));
		handlerAdapter.afterPropertiesSet();
		
		HandlerMethodArgumentResolverComposite composite = (HandlerMethodArgumentResolverComposite) 
			new DirectFieldAccessor(handlerAdapter).getPropertyValue("argumentResolvers");

		List<HandlerMethodArgumentResolver> actual = (List<HandlerMethodArgumentResolver>)
			new DirectFieldAccessor(composite).getPropertyValue("argumentResolvers");
		
		assertTrue(actual.contains(resolver));
		
		composite = (HandlerMethodArgumentResolverComposite) 
		new DirectFieldAccessor(handlerAdapter).getPropertyValue("initBinderArgumentResolvers");

		actual = (List<HandlerMethodArgumentResolver>)
		new DirectFieldAccessor(composite).getPropertyValue("argumentResolvers");

		assertTrue(actual.contains(resolver));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void setCustomReturnValueHandlers() {
		TestHandlerMethodReturnValueHandler handler = new TestHandlerMethodReturnValueHandler();
		handlerAdapter.setCustomReturnValueHandlers(Arrays.<HandlerMethodReturnValueHandler>asList(handler));
		handlerAdapter.afterPropertiesSet();
		
		HandlerMethodReturnValueHandlerComposite composite = (HandlerMethodReturnValueHandlerComposite) 
			new DirectFieldAccessor(handlerAdapter).getPropertyValue("returnValueHandlers");

		List<HandlerMethodReturnValueHandler> actual = (List<HandlerMethodReturnValueHandler>)
			new DirectFieldAccessor(composite).getPropertyValue("returnValueHandlers");
		
		assertTrue(actual.contains(handler));
	}
	
	private HandlerMethod handlerMethod(Object handler, String methodName, Class<?>... paramTypes) throws Exception {
		Method method = handler.getClass().getDeclaredMethod(methodName, paramTypes);
		return new InvocableHandlerMethod(handler, method);
	}

	private final class TestHanderMethodArgumentResolver implements HandlerMethodArgumentResolver {
		public boolean supportsParameter(MethodParameter parameter) {
			return false;
		}

		public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
			return null;
		}
	}

	private final class TestHandlerMethodReturnValueHandler implements HandlerMethodReturnValueHandler{

		public boolean supportsReturnType(MethodParameter returnType) {
			return false;
		}

		public void handleReturnValue(Object returnValue, MethodParameter returnType,
				ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
		}
	}
	
	private static class SimpleHandler {

		@SuppressWarnings("unused")
		public void handle() {
		}
	}

	@SessionAttributes("attr1")
	private static class SessionAttributeHandler {

		@SuppressWarnings("unused")
		public void handle() {
		}
	}

}