/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.core.annotation.AliasFor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;

import static org.junit.Assert.*;

/**
 * Integration tests for {@link ResponseStatusExceptionResolver}.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Rossen Stoyanchev
 */
public class ResponseStatusExceptionResolverTests {

	private final ResponseStatusExceptionResolver exceptionResolver = new ResponseStatusExceptionResolver();

	private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "");

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Before
	public void setup() {
		exceptionResolver.setWarnLogCategory(exceptionResolver.getClass().getName());
	}


	@Test
	public void statusCode() {
		StatusCodeException ex = new StatusCodeException();
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertResolved(mav, 400, null);
	}

	@Test
	public void statusCodeFromComposedResponseStatus() {
		StatusCodeFromComposedResponseStatusException ex = new StatusCodeFromComposedResponseStatusException();
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertResolved(mav, 400, null);
	}

	@Test
	public void statusCodeAndReason() {
		StatusCodeAndReasonException ex = new StatusCodeAndReasonException();
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertResolved(mav, 410, "You suck!");
	}

	@Test
	public void statusCodeAndReasonMessage() {
		Locale locale = Locale.CHINESE;
		LocaleContextHolder.setLocale(locale);
		try {
			StaticMessageSource messageSource = new StaticMessageSource();
			messageSource.addMessage("gone.reason", locale, "Gone reason message");
			exceptionResolver.setMessageSource(messageSource);

			StatusCodeAndReasonMessageException ex = new StatusCodeAndReasonMessageException();
			exceptionResolver.resolveException(request, response, null, ex);
			assertEquals("Invalid status reason", "Gone reason message", response.getErrorMessage());
		}
		finally {
			LocaleContextHolder.resetLocaleContext();
		}
	}

	@Test
	public void notAnnotated() {
		Exception ex = new Exception();
		exceptionResolver.resolveException(request, response, null, ex);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNull("ModelAndView returned", mav);
	}

	@Test // SPR-12903
	public void nestedException() throws Exception {
		Exception cause = new StatusCodeAndReasonMessageException();
		TypeMismatchException ex = new TypeMismatchException("value", ITestBean.class, cause);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertResolved(mav, 410, "gone.reason");
	}

	@Test
	public void responseStatusException() throws Exception {
		ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertResolved(mav, 400, null);
	}

	@Test  // SPR-15524
	public void responseStatusExceptionWithReason() throws Exception {
		ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "The reason");
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertResolved(mav, 400, "The reason");
	}


	private void assertResolved(ModelAndView mav, int status, String reason) {
		assertTrue("No Empty ModelAndView returned", mav != null && mav.isEmpty());
		assertEquals(status, response.getStatus());
		assertEquals(reason, response.getErrorMessage());
		assertTrue(response.isCommitted());
	}


	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@SuppressWarnings("serial")
	private static class StatusCodeException extends Exception {
	}

	@ResponseStatus(code = HttpStatus.GONE, reason = "You suck!")
	@SuppressWarnings("serial")
	private static class StatusCodeAndReasonException extends Exception {
	}

	@ResponseStatus(code = HttpStatus.GONE, reason = "gone.reason")
	@SuppressWarnings("serial")
	private static class StatusCodeAndReasonMessageException extends Exception {
	}

	@ResponseStatus
	@Retention(RetentionPolicy.RUNTIME)
	@SuppressWarnings("unused")
	@interface ComposedResponseStatus {

		@AliasFor(annotation = ResponseStatus.class, attribute = "code")
		HttpStatus responseStatus() default HttpStatus.INTERNAL_SERVER_ERROR;
	}

	@ComposedResponseStatus(responseStatus = HttpStatus.BAD_REQUEST)
	@SuppressWarnings("serial")
	private static class StatusCodeFromComposedResponseStatusException extends Exception {
	}

}
