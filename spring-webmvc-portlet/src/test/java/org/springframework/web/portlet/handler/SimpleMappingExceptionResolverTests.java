/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.portlet.handler;

import java.util.Collections;
import java.util.Properties;
import javax.portlet.WindowState;

import junit.framework.TestCase;

import org.springframework.mock.web.portlet.MockRenderRequest;
import org.springframework.mock.web.portlet.MockRenderResponse;
import org.springframework.web.portlet.ModelAndView;

/**
 * @author Seth Ladd
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class SimpleMappingExceptionResolverTests extends TestCase {

	private static final String DEFAULT_VIEW = "default-view";

	private SimpleMappingExceptionResolver exceptionResolver;
	private MockRenderRequest request;
	private MockRenderResponse response;
	private Object handler1;
	private Object handler2;
	private Exception genericException;

	@Override
	protected void setUp() {
		exceptionResolver = new SimpleMappingExceptionResolver();
		request = new MockRenderRequest();
		response = new MockRenderResponse();
		handler1 = new String();
		handler2 = new Object();
		genericException = new Exception();
	}

	public void testSetOrder() {
		exceptionResolver.setOrder(2);
		assertEquals(2, exceptionResolver.getOrder());
	}

	public void testDefaultErrorView() {
		exceptionResolver.setDefaultErrorView(DEFAULT_VIEW);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals(DEFAULT_VIEW, mav.getViewName());
		assertEquals(genericException, mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE));
	}

	public void testDefaultErrorViewDifferentHandler() {
		exceptionResolver.setDefaultErrorView(DEFAULT_VIEW);
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull("Handler not mapped - ModelAndView should be null", mav);
	}

	public void testDefaultErrorViewDifferentHandlerClass() {
		exceptionResolver.setDefaultErrorView(DEFAULT_VIEW);
		exceptionResolver.setMappedHandlerClasses(new Class[] {String.class});
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull("Handler not mapped - ModelAndView should be null", mav);
	}

	public void testNullDefaultErrorView() {
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertNull("No default error view set - ModelAndView should be null", mav);
	}

	public void testNullExceptionAttribute() {
		exceptionResolver.setDefaultErrorView(DEFAULT_VIEW);
		exceptionResolver.setExceptionAttribute(null);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals(DEFAULT_VIEW, mav.getViewName());
		assertNull(mav.getModel().get(SimpleMappingExceptionResolver.DEFAULT_EXCEPTION_ATTRIBUTE));
	}

	public void testNullExceptionMappings() {
		exceptionResolver.setExceptionMappings(null);
		exceptionResolver.setDefaultErrorView(DEFAULT_VIEW);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals(DEFAULT_VIEW, mav.getViewName());
	}

	public void testDefaultNoRenderWhenMinimized() {
		exceptionResolver.setDefaultErrorView(DEFAULT_VIEW);
		request.setWindowState(WindowState.MINIMIZED);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertNull("Should not render when WindowState is MINIMIZED", mav);
	}

	public void testDoRenderWhenMinimized() {
		exceptionResolver.setDefaultErrorView(DEFAULT_VIEW);
		exceptionResolver.setRenderWhenMinimized(true);
		request.setWindowState(WindowState.MINIMIZED);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertNotNull("ModelAndView should not be null", mav);
		assertEquals(DEFAULT_VIEW, mav.getViewName());
	}

	public void testSimpleExceptionMapping() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	public void testExactExceptionMappingWithHandlerSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	public void testExactExceptionMappingWithHandlerClassSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlerClasses(new Class[] {String.class});
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	public void testExactExceptionMappingWithHandlerInterfaceSpecified() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		exceptionResolver.setMappedHandlerClasses(new Class[] {Comparable.class});
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	public void testSimpleExceptionMappingWithHandlerSpecifiedButWrongHandler() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull("Handler not mapped - ModelAndView should be null", mav);
	}

	public void testSimpleExceptionMappingWithHandlerSpecifiedButWrongHandlerClass() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		exceptionResolver.setMappedHandlerClasses(new Class[] {String.class});
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler2, genericException);
		assertNull("Handler not mapped - ModelAndView should be null", mav);
	}

	public void testMissingExceptionInMapping() {
		Properties props = new Properties();
		props.setProperty("SomeFooThrowable", "error");
		exceptionResolver.setWarnLogCategory("HANDLER_EXCEPTION");
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertNull("Exception not mapped - ModelAndView should be null", mav);
	}

	public void testTwoMappings() {
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("AnotherException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	public void testTwoMappingsOneShortOneLong() {
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("AnotherException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, genericException);
		assertEquals("error", mav.getViewName());
	}

	public void testTwoMappingsOneShortOneLongThrowOddException() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("another-error", mav.getViewName());
	}

	public void testTwoMappingsThrowOddExceptionUseLongExceptionMapping() {
		Exception oddException = new SomeOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("another-error", mav.getViewName());
	}

	public void testThreeMappings() {
		Exception oddException = new AnotherOddException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "another-error");
		props.setProperty("AnotherOddException", "another-some-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("another-some-error", mav.getViewName());
	}

	public void testExceptionWithSubstringMatchingParent() {
		Exception oddException = new SomeOddExceptionChild();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "parent-error");
		props.setProperty("SomeOddExceptionChild", "child-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("child-error", mav.getViewName());
	}

	public void testMostSpecificExceptionInHierarchyWins() {
		Exception oddException = new NoSubstringMatchesThisException();
		Properties props = new Properties();
		props.setProperty("java.lang.Exception", "error");
		props.setProperty("SomeOddException", "parent-error");
		exceptionResolver.setMappedHandlers(Collections.singleton(handler1));
		exceptionResolver.setExceptionMappings(props);
		ModelAndView mav = exceptionResolver.resolveException(request, response, handler1, oddException);
		assertEquals("parent-error", mav.getViewName());
	}


	@SuppressWarnings("serial")
	private static class SomeOddException extends Exception {

	}


	@SuppressWarnings("serial")
	private static class SomeOddExceptionChild extends SomeOddException {

	}


	@SuppressWarnings("serial")
	private static class NoSubstringMatchesThisException extends SomeOddException {

	}


	@SuppressWarnings("serial")
	private static class AnotherOddException extends Exception {

	}

}
