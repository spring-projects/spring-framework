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

import static org.junit.Assert.assertSame;

import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Test fixture for {@link ErrorsMethodArgumentResolver} unit tests.
 * 
 * @author Rossen Stoyanchev
 */
public class ErrorsMethodHandlerArgumentResolverTests {

	private NativeWebRequest webRequest;

	private ErrorsMethodArgumentResolver resolver;

	private MethodParameter errorsParam;

	@Before
	public void setUp() throws Exception {
		this.webRequest = new ServletWebRequest(new MockHttpServletRequest());

		this.resolver = new ErrorsMethodArgumentResolver();

		Method method = this.getClass().getDeclaredMethod("handle", Errors.class);
		this.errorsParam = new MethodParameter(method, 0);
	}

	@Test
	public void supports() throws Exception {
		resolver.supportsParameter(errorsParam);
	}

	@Test
	public void bindingResult() throws Exception {
		WebDataBinder dataBinder = new WebDataBinder(new Object(), "attr");
		BindingResult bindingResult = dataBinder.getBindingResult();

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAttribute("ignore1", "value1");
		mavContainer.addAttribute("ignore2", "value2");
		mavContainer.addAttribute("ignore3", "value3");
		mavContainer.addAttribute("ignore4", "value4");
		mavContainer.addAttribute("ignore5", "value5");
		mavContainer.addAllAttributes(bindingResult.getModel()); // Predictable iteration order of model keys important!
		
		Object actual = resolver.resolveArgument(errorsParam, mavContainer, webRequest, null);

		assertSame(actual, bindingResult);
	}

	@Test
	public void bindingResultExistingModelAttribute() throws Exception {
		Object target1 = new Object();
		WebDataBinder binder1 = new WebDataBinder(target1, "attr1");
		BindingResult bindingResult1 = binder1.getBindingResult();

		Object target2 = new Object();
		WebDataBinder binder2 = new WebDataBinder(target1, "attr2");
		BindingResult bindingResult2 = binder2.getBindingResult();

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAttribute("attr1", target1);
		mavContainer.addAttribute("attr2", target2);
		mavContainer.addAttribute("filler", "fillerValue");
		mavContainer.addAllAttributes(bindingResult1.getModel()); 
		mavContainer.addAllAttributes(bindingResult2.getModel()); 
		
		Object actual = resolver.resolveArgument(errorsParam, mavContainer, webRequest, null);

		assertSame("Should resolve to the latest BindingResult added", actual, bindingResult2);
	}

	@Test(expected=IllegalStateException.class)
	public void bindingResultNotFound() throws Exception {
		WebDataBinder dataBinder = new WebDataBinder(new Object(), "attr");
		BindingResult bindingResult = dataBinder.getBindingResult();

		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAllAttributes(bindingResult.getModel());
		mavContainer.addAttribute("ignore1", "value1");
		
		resolver.resolveArgument(errorsParam, mavContainer, webRequest, null);
	}
	
	@Test(expected=IllegalStateException.class)
	public void noBindingResult() throws Exception {
		resolver.resolveArgument(errorsParam, new ModelAndViewContainer(), webRequest, null);
	}

	@SuppressWarnings("unused")
	private void handle(Errors errors) {
	}

}
