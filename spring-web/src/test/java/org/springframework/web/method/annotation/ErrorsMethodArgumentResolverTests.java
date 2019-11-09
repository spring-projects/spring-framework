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

package org.springframework.web.method.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture with {@link ErrorsMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
public class ErrorsMethodArgumentResolverTests {

	private final ErrorsMethodArgumentResolver resolver = new ErrorsMethodArgumentResolver();

	private BindingResult bindingResult;

	private MethodParameter paramErrors;

	private NativeWebRequest webRequest;


	@BeforeEach
	public void setup() throws Exception {
		paramErrors = new MethodParameter(getClass().getDeclaredMethod("handle", Errors.class), 0);
		bindingResult = new WebDataBinder(new Object(), "attr").getBindingResult();
		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	public void supports() {
		resolver.supportsParameter(paramErrors);
	}

	@Test
	public void bindingResult() throws Exception {
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAttribute("ignore1", "value1");
		mavContainer.addAttribute("ignore2", "value2");
		mavContainer.addAttribute("ignore3", "value3");
		mavContainer.addAttribute("ignore4", "value4");
		mavContainer.addAttribute("ignore5", "value5");
		mavContainer.addAllAttributes(bindingResult.getModel());

		Object actual = resolver.resolveArgument(paramErrors, mavContainer, webRequest, null);
		assertThat(bindingResult).isSameAs(actual);
	}

	@Test
	public void bindingResultNotFound() throws Exception {
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAllAttributes(bindingResult.getModel());
		mavContainer.addAttribute("ignore1", "value1");

		assertThatIllegalStateException().isThrownBy(() ->
				resolver.resolveArgument(paramErrors, mavContainer, webRequest, null));
	}

	@Test
	public void noBindingResult() throws Exception {
		assertThatIllegalStateException().isThrownBy(() ->
				resolver.resolveArgument(paramErrors, new ModelAndViewContainer(), webRequest, null));
	}


	@SuppressWarnings("unused")
	private void handle(Errors errors) {
	}

}
