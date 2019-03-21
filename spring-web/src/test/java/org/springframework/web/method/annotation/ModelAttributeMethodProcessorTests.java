/*
 * Copyright 2002-2015 the original author or authors.
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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.SynthesizingMethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.tests.sample.beans.TestBean;
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
import org.springframework.web.method.support.ModelAndViewContainer;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Test fixture with {@link ModelAttributeMethodProcessor}.
 *
 * @author Rossen Stoyanchev
 */
public class ModelAttributeMethodProcessorTests {

	private NativeWebRequest request;

	private ModelAndViewContainer container;

	private ModelAttributeMethodProcessor processor;

	private MethodParameter paramNamedValidModelAttr;
	private MethodParameter paramErrors;
	private MethodParameter paramInt;
	private MethodParameter paramModelAttr;
	private MethodParameter paramBindingDisabledAttr;
	private MethodParameter paramNonSimpleType;

	private MethodParameter returnParamNamedModelAttr;
	private MethodParameter returnParamNonSimpleType;


	@Before
	public void setUp() throws Exception {
		this.request = new ServletWebRequest(new MockHttpServletRequest());
		this.container = new ModelAndViewContainer();
		this.processor = new ModelAttributeMethodProcessor(false);

		Method method = ModelAttributeHandler.class.getDeclaredMethod("modelAttribute",
				TestBean.class, Errors.class, int.class, TestBean.class,
				TestBean.class, TestBean.class);

		this.paramNamedValidModelAttr = new SynthesizingMethodParameter(method, 0);
		this.paramErrors = new SynthesizingMethodParameter(method, 1);
		this.paramInt = new SynthesizingMethodParameter(method, 2);
		this.paramModelAttr = new SynthesizingMethodParameter(method, 3);
		this.paramBindingDisabledAttr = new SynthesizingMethodParameter(method, 4);
		this.paramNonSimpleType = new SynthesizingMethodParameter(method, 5);

		method = getClass().getDeclaredMethod("annotatedReturnValue");
		this.returnParamNamedModelAttr = new MethodParameter(method, -1);

		method = getClass().getDeclaredMethod("notAnnotatedReturnValue");
		this.returnParamNonSimpleType = new MethodParameter(method, -1);
	}


	@Test
	public void supportedParameters() throws Exception {
		assertTrue(this.processor.supportsParameter(this.paramNamedValidModelAttr));
		assertTrue(this.processor.supportsParameter(this.paramModelAttr));

		assertFalse(this.processor.supportsParameter(this.paramErrors));
		assertFalse(this.processor.supportsParameter(this.paramInt));
		assertFalse(this.processor.supportsParameter(this.paramNonSimpleType));
	}

	@Test
	public void supportedParametersInDefaultResolutionMode() throws Exception {
		processor = new ModelAttributeMethodProcessor(true);

		// Only non-simple types, even if not annotated
		assertTrue(this.processor.supportsParameter(this.paramNamedValidModelAttr));
		assertTrue(this.processor.supportsParameter(this.paramErrors));
		assertTrue(this.processor.supportsParameter(this.paramModelAttr));
		assertTrue(this.processor.supportsParameter(this.paramNonSimpleType));

		assertFalse(this.processor.supportsParameter(this.paramInt));
	}

	@Test
	public void supportedReturnTypes() throws Exception {
		processor = new ModelAttributeMethodProcessor(false);
		assertTrue(this.processor.supportsReturnType(returnParamNamedModelAttr));
		assertFalse(this.processor.supportsReturnType(returnParamNonSimpleType));
	}

	@Test
	public void supportedReturnTypesInDefaultResolutionMode() throws Exception {
		processor = new ModelAttributeMethodProcessor(true);
		assertTrue(this.processor.supportsReturnType(returnParamNamedModelAttr));
		assertTrue(this.processor.supportsReturnType(returnParamNonSimpleType));
	}

	@Test
	public void bindExceptionRequired() throws Exception {
		assertTrue(this.processor.isBindExceptionRequired(null, this.paramNonSimpleType));
		assertFalse(this.processor.isBindExceptionRequired(null, this.paramNamedValidModelAttr));
	}

	@Test
	public void resolveArgumentFromModel() throws Exception {
		testGetAttributeFromModel("attrName", this.paramNamedValidModelAttr);
		testGetAttributeFromModel("testBean", this.paramModelAttr);
		testGetAttributeFromModel("testBean", this.paramNonSimpleType);
	}

	@Test
	public void resovleArgumentViaDefaultConstructor() throws Exception {
		WebDataBinder dataBinder = new WebRequestDataBinder(null);
		WebDataBinderFactory factory = mock(WebDataBinderFactory.class);
		given(factory.createBinder(anyObject(), notNull(), eq("attrName"))).willReturn(dataBinder);

		this.processor.resolveArgument(this.paramNamedValidModelAttr, this.container, this.request, factory);
		verify(factory).createBinder(anyObject(), notNull(), eq("attrName"));
	}

	@Test
	public void resolveArgumentValidation() throws Exception {
		String name = "attrName";
		Object target = new TestBean();
		this.container.addAttribute(name, target);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
		WebDataBinderFactory factory = mock(WebDataBinderFactory.class);
		given(factory.createBinder(this.request, target, name)).willReturn(dataBinder);

		this.processor.resolveArgument(this.paramNamedValidModelAttr, this.container, this.request, factory);

		assertTrue(dataBinder.isBindInvoked());
		assertTrue(dataBinder.isValidateInvoked());
	}

	@Test
	public void resolveArgumentBindingDisabledPreviously() throws Exception {
		String name = "attrName";
		Object target = new TestBean();
		this.container.addAttribute(name, target);

		// Declare binding disabled (e.g. via @ModelAttribute method)
		this.container.setBindingDisabled(name);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
		WebDataBinderFactory factory = mock(WebDataBinderFactory.class);
		given(factory.createBinder(this.request, target, name)).willReturn(dataBinder);

		this.processor.resolveArgument(this.paramNamedValidModelAttr, this.container, this.request, factory);

		assertFalse(dataBinder.isBindInvoked());
		assertTrue(dataBinder.isValidateInvoked());
	}

	@Test
	public void resolveArgumentBindingDisabled() throws Exception {
		String name = "noBindAttr";
		Object target = new TestBean();
		this.container.addAttribute(name, target);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
		WebDataBinderFactory factory = mock(WebDataBinderFactory.class);
		given(factory.createBinder(this.request, target, name)).willReturn(dataBinder);

		this.processor.resolveArgument(this.paramBindingDisabledAttr, this.container, this.request, factory);

		assertFalse(dataBinder.isBindInvoked());
		assertTrue(dataBinder.isValidateInvoked());
	}

	@Test(expected = BindException.class)
	public void resolveArgumentBindException() throws Exception {
		String name = "testBean";
		Object target = new TestBean();
		this.container.getModel().addAttribute(target);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(target, name);
		dataBinder.getBindingResult().reject("error");
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.request, target, name)).willReturn(dataBinder);

		this.processor.resolveArgument(this.paramNonSimpleType, this.container, this.request, binderFactory);
		verify(binderFactory).createBinder(this.request, target, name);
	}

	@Test  // SPR-9378
	public void resolveArgumentOrdering() throws Exception {
		String name = "testBean";
		Object testBean = new TestBean(name);
		this.container.addAttribute(name, testBean);
		this.container.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, testBean);

		Object anotherTestBean = new TestBean();
		this.container.addAttribute("anotherTestBean", anotherTestBean);

		StubRequestDataBinder dataBinder = new StubRequestDataBinder(testBean, name);
		WebDataBinderFactory binderFactory = mock(WebDataBinderFactory.class);
		given(binderFactory.createBinder(this.request, testBean, name)).willReturn(dataBinder);

		this.processor.resolveArgument(this.paramModelAttr, this.container, this.request, binderFactory);

		Object[] values = this.container.getModel().values().toArray();
		assertSame("Resolved attribute should be updated to be last", testBean, values[1]);
		assertSame("BindingResult of resolved attr should be last", dataBinder.getBindingResult(), values[2]);
	}

	@Test
	public void handleAnnotatedReturnValue() throws Exception {
		this.processor.handleReturnValue("expected", this.returnParamNamedModelAttr, this.container, this.request);
		assertEquals("expected", this.container.getModel().get("modelAttrName"));
	}

	@Test
	public void handleNotAnnotatedReturnValue() throws Exception {
		TestBean testBean = new TestBean("expected");
		this.processor.handleReturnValue(testBean, this.returnParamNonSimpleType, this.container, this.request);
		assertSame(testBean, this.container.getModel().get("testBean"));
	}


	private void testGetAttributeFromModel(String expectedAttrName, MethodParameter param) throws Exception {
		Object target = new TestBean();
		this.container.addAttribute(expectedAttrName, target);

		WebDataBinder dataBinder = new WebRequestDataBinder(target);
		WebDataBinderFactory factory = mock(WebDataBinderFactory.class);
		given(factory.createBinder(this.request, target, expectedAttrName)).willReturn(dataBinder);

		this.processor.resolveArgument(param, this.container, this.request, factory);
		verify(factory).createBinder(this.request, target, expectedAttrName);
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


	@Target({METHOD, FIELD, CONSTRUCTOR, PARAMETER})
	@Retention(RUNTIME)
	public @interface Valid {
	}


	@SessionAttributes(types=TestBean.class)
	private static class ModelAttributeHandler {

		@SuppressWarnings("unused")
		public void modelAttribute(
				@ModelAttribute("attrName") @Valid TestBean annotatedAttr,
				Errors errors,
				int intArg,
				@ModelAttribute TestBean defaultNameAttr,
				@ModelAttribute(name="noBindAttr", binding=false) @Valid TestBean noBindAttr,
				TestBean notAnnotatedAttr) {
		}
	}


	@ModelAttribute("modelAttrName") @SuppressWarnings("unused")
	private String annotatedReturnValue() {
		return null;
	}


	@SuppressWarnings("unused")
	private TestBean notAnnotatedReturnValue() {
		return null;
	}

}
