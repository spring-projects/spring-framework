package org.springframework.web.servlet.mvc.annotation;

import static org.junit.Assert.*;

import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.web.servlet.ModelAndView;

/** @author Arjen Poutsma */
public class ResponseStatusExceptionResolverTests {

	private ResponseStatusExceptionResolver exceptionResolver;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@Before
	public void setUp() {
		exceptionResolver = new ResponseStatusExceptionResolver();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		request.setMethod("GET");
	}

	@Test
	public void statusCode() {
		StatusCodeException ex = new StatusCodeException();
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 400, response.getStatus());
	}

	@Test
	public void statusCodeAndReason() {
		StatusCodeAndReasonException ex = new StatusCodeAndReasonException();
		ModelAndView mav = exceptionResolver.resolveException(request, response, null, ex);
		assertNotNull("No ModelAndView returned", mav);
		assertTrue("No Empty ModelAndView returned", mav.isEmpty());
		assertEquals("Invalid status code", 410, response.getStatus());
		assertEquals("Invalid status reason", "You suck!", response.getErrorMessage());
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

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@SuppressWarnings("serial")
	private static class StatusCodeException extends Exception {

	}

	@ResponseStatus(value = HttpStatus.GONE, reason = "You suck!")
	@SuppressWarnings("serial")
	private static class StatusCodeAndReasonException extends Exception {

	}

	@ResponseStatus(value = HttpStatus.GONE, reason = "gone.reason")
	@SuppressWarnings("serial")
	private static class StatusCodeAndReasonMessageException extends Exception {

	}

}
