/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.method.annotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test fixture with {@link ErrorsMethodArgumentResolver}.
 *
 * @author Rossen Stoyanchev
 */
class ErrorsMethodArgumentResolverTests {

	private final ErrorsMethodArgumentResolver resolver = new ErrorsMethodArgumentResolver();

	private BindingResult bindingResult;

	private MethodParameter paramErrors;

	private NativeWebRequest webRequest;


	@BeforeEach
	void setup() throws Exception {
		paramErrors = new MethodParameter(getClass().getDeclaredMethod("handle", Errors.class), 0);
		bindingResult = new WebDataBinder(new Object(), "attr").getBindingResult();
		webRequest = new ServletWebRequest(new MockHttpServletRequest());
	}


	@Test
	void supports() {
		resolver.supportsParameter(paramErrors);
	}

	@Test
	void bindingResult() throws Exception {
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
	void bindingResultNotFound() {
		ModelAndViewContainer mavContainer = new ModelAndViewContainer();
		mavContainer.addAllAttributes(bindingResult.getModel());
		mavContainer.addAttribute("ignore1", "value1");

		assertThatIllegalStateException().isThrownBy(() ->
				resolver.resolveArgument(paramErrors, mavContainer, webRequest, null));
	}

	@Test
	void noBindingResult() {
		assertThatIllegalStateException().isThrownBy(() ->
				resolver.resolveArgument(paramErrors, new ModelAndViewContainer(), webRequest, null));
	}


	@SuppressWarnings("unused")
	private void handle(Errors errors) {
	}

}
