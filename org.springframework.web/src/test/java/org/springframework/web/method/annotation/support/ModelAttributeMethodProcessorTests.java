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

package org.springframework.web.method.annotation.support;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test fixture for {@link ModelAttributeMethodProcessor} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class ModelAttributeMethodProcessorTests {

	private ModelAttributeMethodProcessor processor;
	
	private MethodParameter annotatedParam;

	private MethodParameter integerParam;

	private MethodParameter defaultNameParam;

	private MethodParameter notAnnotatedParam;
	
	private MethodParameter annotatedReturnParam;
	
	private MethodParameter notAnnotatedReturnParam;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;
	
	@Before
	public void setUp() throws Exception {
		this.processor = new ModelAttributeMethodProcessor(true);

		Class<?> handlerType = ModelAttributeHandler.class;
		Method method = handlerType.getDeclaredMethod("modelAttribute", TestBean.class, Errors.class, int.class,
				TestBean.class, TestBean.class);
		
		this.annotatedParam = new MethodParameter(method, 0);
		this.integerParam = new MethodParameter(method, 2);
		this.defaultNameParam = new MethodParameter(method, 3);
		this.notAnnotatedParam = new MethodParameter(method, 4);
		
		this.annotatedReturnParam = new MethodParameter(getClass().getDeclaredMethod("annotatedReturnValue"), -1);
		this.notAnnotatedReturnParam = new MethodParameter(getClass().getDeclaredMethod("notAnnotatedReturnValue"), -1);
		
		mavContainer = new ModelAndViewContainer();

		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void supportParameter() throws Exception {
		assertTrue(processor.supportsParameter(annotatedParam));
		assertFalse(processor.supportsParameter(integerParam));
		assertTrue(processor.supportsParameter(notAnnotatedParam));
		
		this.processor = new ModelAttributeMethodProcessor(false);
		assertFalse(processor.supportsParameter(notAnnotatedParam));
	}
	
	@Test
	public void supportsReturnType() throws Exception {
		assertTrue(processor.supportsReturnType(annotatedReturnParam));
		assertFalse(processor.supportsReturnType(notAnnotatedReturnParam));
	}
	
	@Test
	public void shouldValidate() throws Exception {
		assertTrue(processor.shouldValidate(annotatedParam));
		assertFalse(processor.shouldValidate(notAnnotatedParam));
	}

	@Test
	public void failOnError() throws Exception {
		assertFalse(processor.failOnError(annotatedParam));
		assertTrue(processor.failOnError(notAnnotatedParam));
	}

	@Test
	public void createBinderFromModelAttribute() throws Exception {
		createBinderFromModelAttr("attrName", annotatedParam);
		createBinderFromModelAttr("testBean", defaultNameParam);
		createBinderFromModelAttr("testBean", notAnnotatedParam);
	}

	private void createBinderFromModelAttr(String expectedAttrName, MethodParameter param) throws Exception {
		Object target = new TestBean();
		mavContainer.addAttribute(expectedAttrName, target);

		WebDataBinder dataBinder = new WebRequestDataBinder(null);

		WebDataBinderFactory binderFactory = createMock(WebDataBinderFactory.class);
		expect(binderFactory.createBinder(webRequest, target, expectedAttrName)).andReturn(dataBinder);
		replay(binderFactory);
		
		processor.resolveArgument(param, mavContainer, webRequest, binderFactory);
		
		verify(binderFactory);
	}

	@Test
	public void createBinderWithAttributeConstructor() throws Exception {
		WebDataBinder dataBinder = new WebRequestDataBinder(null);

		WebDataBinderFactory factory = createMock(WebDataBinderFactory.class);
		expect(factory.createBinder((NativeWebRequest) anyObject(), notNull(), eq("attrName"))).andReturn(dataBinder);
		replay(factory);
		
		processor.resolveArgument(annotatedParam, mavContainer, webRequest, factory);
		
		verify(factory);
	}

	@Test
	public void bindAndValidate() throws Exception {
		Object target = new TestBean();
		mavContainer.addAttribute("attrName", target);
		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target);

		WebDataBinderFactory binderFactory = createMock(WebDataBinderFactory.class);
		expect(binderFactory.createBinder(webRequest, target, "attrName")).andReturn(dataBinder);
		replay(binderFactory);
		
		processor.resolveArgument(annotatedParam, mavContainer, webRequest, binderFactory);

		assertTrue(dataBinder.isBindInvoked());
		assertTrue(dataBinder.isValidateInvoked());
	}
	
	@Test(expected=BindException.class)
	public void bindAndFail() throws Exception {
		Object target = new TestBean();
		mavContainer.getModel().addAttribute(target);
		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target);
		dataBinder.getBindingResult().reject("error");

		WebDataBinderFactory binderFactory = createMock(WebDataBinderFactory.class);
		expect(binderFactory.createBinder(webRequest, target, "testBean")).andReturn(dataBinder);
		replay(binderFactory);
		
		processor.resolveArgument(notAnnotatedParam, mavContainer, webRequest, binderFactory);
	}
	
	@Test
	public void handleAnnotatedReturnValue() throws Exception {
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		processor.handleReturnValue("expected", annotatedReturnParam, mavContainer, webRequest);

		assertEquals("expected", mavContainer.getModel().get("modelAttrName"));
	}

	@Test
	public void handleNotAnnotatedReturnValue() throws Exception {
		TestBean testBean = new TestBean("expected");
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		processor.handleReturnValue(testBean, notAnnotatedReturnParam, mavContainer, webRequest);
		
		assertSame(testBean, mavContainer.getModel().get("testBean"));
	}
	

	@SessionAttributes(types=TestBean.class)
	private static class ModelAttributeHandler {

		@SuppressWarnings("unused")
		public void modelAttribute(@ModelAttribute("attrName") @Valid TestBean annotatedAttr, 
								   Errors errors,
								   int intArg,
								   @ModelAttribute TestBean defaultNameAttr,
								   TestBean notAnnotatedAttr) {
		}
	}

	@SuppressWarnings("unused")
	@ModelAttribute("modelAttrName")
	private String annotatedReturnValue() {
		return null;
	}

	@SuppressWarnings("unused")
	private TestBean notAnnotatedReturnValue() {
		return null;
	}
	
	private static class StubRequestDataBinder extends WebRequestDataBinder {
		
		private boolean bindInvoked;
		
		private boolean validateInvoked;

		public StubRequestDataBinder(Object target) {
			super(target);
		}

		public boolean isBindInvoked() {
			return bindInvoked;
		}

		public boolean isValidateInvoked() {
			return validateInvoked;
		}

		@Override
		public void bind(WebRequest request) {
			this.bindInvoked = true;
		}

		@Override
		public void validate() {
			this.validateInvoked = true;
		}
	}

	@Target({ METHOD, FIELD, CONSTRUCTOR, PARAMETER })
	@Retention(RUNTIME)
	public @interface Valid {
	}
	
}
