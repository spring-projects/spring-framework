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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.TestBean;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.bind.support.WebRequestDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.method.support.ModelAndViewContainer;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

/**
 * Test fixture with {@link ModelAttributeMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAttributeMethodProcessorTests {

	private ModelAttributeMethodProcessor processor;

	private MethodParameter paramNamedValidModelAttr;

	private MethodParameter paramErrors;

	private MethodParameter paramInt;

	private MethodParameter paramModelAttr;

	private MethodParameter paramNonSimpleType;

	private MethodParameter returnParamNamedModelAttr;

	private MethodParameter returnParamNonSimpleType;

	private ModelAndViewContainer mavContainer;

	private NativeWebRequest webRequest;

	@Before
	public void setUp() throws Exception {
		processor = new ModelAttributeMethodProcessor(false);

		Method method = ModelAttributeHandler.class.getDeclaredMethod("modelAttribute",
				TestBean.class, Errors.class, int.class, TestBean.class, TestBean.class);

		paramNamedValidModelAttr = new MethodParameter(method, 0);
		paramErrors = new MethodParameter(method, 1);
		paramInt = new MethodParameter(method, 2);
		paramModelAttr = new MethodParameter(method, 3);
		paramNonSimpleType = new MethodParameter(method, 4);

		returnParamNamedModelAttr = new MethodParameter(getClass().getDeclaredMethod("annotatedReturnValue"), -1);
		returnParamNonSimpleType = new MethodParameter(getClass().getDeclaredMethod("notAnnotatedReturnValue"), -1);

		mavContainer = new ModelAndViewContainer();

		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}

	@Test
	public void supportedParameters() throws Exception {
		// Only @ModelAttribute arguments
		assertTrue(processor.supportsParameter(paramNamedValidModelAttr));
		assertTrue(processor.supportsParameter(paramModelAttr));

		assertFalse(processor.supportsParameter(paramErrors));
		assertFalse(processor.supportsParameter(paramInt));
		assertFalse(processor.supportsParameter(paramNonSimpleType));
	}

	@Test
	public void supportedParametersInDefaultResolutionMode() throws Exception {
		processor = new ModelAttributeMethodProcessor(true);

		// Only non-simple types, even if not annotated
		assertTrue(processor.supportsParameter(paramNamedValidModelAttr));
		assertTrue(processor.supportsParameter(paramErrors));
		assertTrue(processor.supportsParameter(paramModelAttr));
		assertTrue(processor.supportsParameter(paramNonSimpleType));

		assertFalse(processor.supportsParameter(paramInt));
	}

	@Test
	public void supportedReturnTypes() throws Exception {
		processor = new ModelAttributeMethodProcessor(false);
		assertTrue(processor.supportsReturnType(returnParamNamedModelAttr));
		assertFalse(processor.supportsReturnType(returnParamNonSimpleType));
	}

	@Test
	public void supportedReturnTypesInDefaultResolutionMode() throws Exception {
		processor = new ModelAttributeMethodProcessor(true);
		assertTrue(processor.supportsReturnType(returnParamNamedModelAttr));
		assertTrue(processor.supportsReturnType(returnParamNonSimpleType));
	}

	@Test
	public void bindExceptionRequired() throws Exception {
		assertTrue(processor.isBindExceptionRequired(null, paramNonSimpleType));
	}

	@Test
	public void bindExceptionNotRequired() throws Exception {
		assertFalse(processor.isBindExceptionRequired(null, paramNamedValidModelAttr));
	}

	@Test
	public void resovleArgumentFromModel() throws Exception {
		getAttributeFromModel("attrName", paramNamedValidModelAttr);
		getAttributeFromModel("testBean", paramModelAttr);
		getAttributeFromModel("testBean", paramNonSimpleType);
	}

	private void getAttributeFromModel(String expectedAttributeName, MethodParameter param) throws Exception {
		Object target = new TestBean();
		mavContainer.addAttribute(expectedAttributeName, target);

		WebDataBinder dataBinder = new WebRequestDataBinder(target);
		WebDataBinderFactory factory = createMock(WebDataBinderFactory.class);
		expect(factory.createBinder(webRequest, target, expectedAttributeName)).andReturn(dataBinder);
		replay(factory);

		processor.resolveArgument(param, mavContainer, webRequest, factory);

		verify(factory);
	}

	@Test
	public void resovleArgumentViaDefaultConstructor() throws Exception {
		WebDataBinder dataBinder = new WebRequestDataBinder(null);

		WebDataBinderFactory factory = createMock(WebDataBinderFactory.class);
		expect(factory.createBinder((NativeWebRequest) anyObject(), notNull(), eq("attrName"))).andReturn(dataBinder);
		replay(factory);

		processor.resolveArgument(paramNamedValidModelAttr, mavContainer, webRequest, factory);

		verify(factory);
	}

	@Test
	public void resovleArgumentValidation() throws Exception {
		String name = "attrName";
		Object target = new TestBean();
		mavContainer.addAttribute(name, target);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
		WebDataBinderFactory binderFactory = createMock(WebDataBinderFactory.class);
		expect(binderFactory.createBinder(webRequest, target, name)).andReturn(dataBinder);
		replay(binderFactory);

		processor.resolveArgument(paramNamedValidModelAttr, mavContainer, webRequest, binderFactory);

		assertTrue(dataBinder.isBindInvoked());
		assertTrue(dataBinder.isValidateInvoked());
	}

	@Test(expected=BindException.class)
	public void resovleArgumentBindException() throws Exception {
		String name = "testBean";
		Object target = new TestBean();
		mavContainer.getModel().addAttribute(target);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
		dataBinder.getBindingResult().reject("error");

		WebDataBinderFactory binderFactory = createMock(WebDataBinderFactory.class);
		expect(binderFactory.createBinder(webRequest, target, name)).andReturn(dataBinder);
		replay(binderFactory);

		processor.resolveArgument(paramNonSimpleType, mavContainer, webRequest, binderFactory);
	}

	// SPR-9378

	@Test
	public void resolveArgumentOrdering() throws Exception {
		String name = "testBean";
		Object testBean = new TestBean(name);
		mavContainer.addAttribute(name, testBean);
		mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, testBean);

		Object anotherTestBean = new TestBean();
		mavContainer.addAttribute("anotherTestBean", anotherTestBean);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(testBean, name);
		WebDataBinderFactory binderFactory = createMock(WebDataBinderFactory.class);
		expect(binderFactory.createBinder(webRequest, testBean, name)).andReturn(dataBinder);
		replay(binderFactory);

		processor.resolveArgument(paramModelAttr, mavContainer, webRequest, binderFactory);

		assertSame("Resolved attribute should be updated to be last in the order",
				testBean, mavContainer.getModel().values().toArray()[1]);
		assertSame("BindingResult of resolved attribute should be last in the order",
				dataBinder.getBindingResult(), mavContainer.getModel().values().toArray()[2]);
	}

	@Test
	public void handleAnnotatedReturnValue() throws Exception {
		processor.handleReturnValue("expected", returnParamNamedModelAttr, mavContainer, webRequest);
		assertEquals("expected", mavContainer.getModel().get("modelAttrName"));
	}

	@Test
	public void handleNotAnnotatedReturnValue() throws Exception {
		TestBean testBean = new TestBean("expected");
		processor.handleReturnValue(testBean, returnParamNonSimpleType, mavContainer, webRequest);

		assertSame(testBean, mavContainer.getModel().get("testBean"));
	}

	private static class StubRequestDataBinder extends WebRequestDataBinder {

		private boolean bindInvoked;

		private boolean validateInvoked;

		public StubRequestDataBinder(Object target, String objectName) {
			super(target, objectName);
		}

		public boolean isBindInvoked() {
			return bindInvoked;
		}

		public boolean isValidateInvoked() {
			return validateInvoked;
		}

		@Override
		public void bind(WebRequest request) {
			bindInvoked = true;
		}

		@Override
		public void validate() {
			validateInvoked = true;
		}

		@Override
		public void validate(Object... validationHints) {
			validateInvoked = true;
		}


	}

	@Target({ METHOD, FIELD, CONSTRUCTOR, PARAMETER })
	@Retention(RUNTIME)
	public @interface Valid {
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

}