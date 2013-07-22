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

package org.springframework.web.servlet.i18n;

import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.TimeZoneResolver;

/**
 * @author Nicholas Williams
 */
public class TimeZoneChangeInterceptorTests extends TestCase {

	public void testParamName() {
		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();

		assertEquals("The param name is not correct.", "timezone", interceptor.getParamName());

		interceptor.setParamName("fooTz1");
		assertEquals("The param name did not change.", "fooTz1", interceptor.getParamName());
	}

	public void testPreHandleNoParameter() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		assertTrue("The return value is not correct.", interceptor.preHandle(request, null, null));
	}

	public void testPreHandleBadParameter() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(TimeZoneChangeInterceptor.DEFAULT_PARAM_NAME, "America/Fake");

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		assertTrue("The return value is not correct.", interceptor.preHandle(request, null, null));
	}

	public void testPreHandleNoResolver() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(TimeZoneChangeInterceptor.DEFAULT_PARAM_NAME, "America/Chicago");

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		try {
			interceptor.preHandle(request, null, null);
			fail("Expected IllegalStateException, got no exception.");
		}
		catch (IllegalStateException e) {
			// good
		}
	}

	public void testPreHandleNoResolverWithCustomParam() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo1", "America/Chicago");

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		interceptor.setParamName("foo1");
		try {
			interceptor.preHandle(request, null, null);
			fail("Expected IllegalStateException, got no exception.");
		}
		catch (IllegalStateException e) {
			// good
		}
	}

	public void testPreHandleAmericaChicago() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(TimeZoneChangeInterceptor.DEFAULT_PARAM_NAME, "America/Chicago");

		MockTimeZoneResolver resolver = new MockTimeZoneResolver();
		request.setAttribute(DispatcherServlet.TIME_ZONE_RESOLVER_ATTRIBUTE, resolver);

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		assertTrue("The return value is not correct.", interceptor.preHandle(request, null, null));

		assertSame("The request is not correct.", request, resolver.getSetTimeZoneRequest());
		assertEquals("The time zone is not correct.", "America/Chicago",
				resolver.getSetTimeZoneTimeZone().getID());
	}

	public void testPreHandleAmericaLosAngeles() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter(TimeZoneChangeInterceptor.DEFAULT_PARAM_NAME, "America/Los_Angeles");

		MockTimeZoneResolver resolver = new MockTimeZoneResolver();
		request.setAttribute(DispatcherServlet.TIME_ZONE_RESOLVER_ATTRIBUTE, resolver);

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		assertTrue("The return value is not correct.", interceptor.preHandle(request, null, null));

		assertSame("The request is not correct.", request, resolver.getSetTimeZoneRequest());
		assertEquals("The time zone is not correct.", "America/Los_Angeles",
				resolver.getSetTimeZoneTimeZone().getID());
	}

	public void testPreHandleAmericaChicagoWithCustomParam() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("foo1", "America/Chicago");

		MockTimeZoneResolver resolver = new MockTimeZoneResolver();
		request.setAttribute(DispatcherServlet.TIME_ZONE_RESOLVER_ATTRIBUTE, resolver);

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		interceptor.setParamName("foo1");
		assertTrue("The return value is not correct.", interceptor.preHandle(request, null, null));

		assertSame("The request is not correct.", request, resolver.getSetTimeZoneRequest());
		assertEquals("The time zone is not correct.", "America/Chicago",
				resolver.getSetTimeZoneTimeZone().getID());
	}

	public void testPreHandleAmericaLosAngelesWithCustomParam() throws ServletException {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addParameter("bar2", "America/Los_Angeles");

		MockTimeZoneResolver resolver = new MockTimeZoneResolver();
		request.setAttribute(DispatcherServlet.TIME_ZONE_RESOLVER_ATTRIBUTE, resolver);

		TimeZoneChangeInterceptor interceptor = new TimeZoneChangeInterceptor();
		interceptor.setParamName("bar2");
		assertTrue("The return value is not correct.", interceptor.preHandle(request, null, null));

		assertSame("The request is not correct.", request, resolver.getSetTimeZoneRequest());
		assertEquals("The time zone is not correct.", "America/Los_Angeles",
				resolver.getSetTimeZoneTimeZone().getID());
	}


	private static class MockTimeZoneResolver implements TimeZoneResolver {

		private HttpServletRequest setTimeZoneRequest;

		private TimeZone setTimeZoneTimeZone;

		@Override
		public TimeZone resolveTimeZone(HttpServletRequest request) {
			throw new UnsupportedOperationException("Can't resolve time zone in this mock.");
		}

		@Override
		public void setTimeZone(HttpServletRequest request, HttpServletResponse response, TimeZone timeZone) {
			this.setTimeZoneRequest = request;
			this.setTimeZoneTimeZone = timeZone;
		}

		private HttpServletRequest getSetTimeZoneRequest() {
			return setTimeZoneRequest;
		}

		private TimeZone getSetTimeZoneTimeZone() {
			return setTimeZoneTimeZone;
		}

	}

}
