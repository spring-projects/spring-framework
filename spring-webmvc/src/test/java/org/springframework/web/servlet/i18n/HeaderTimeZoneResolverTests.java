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

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockHttpServletRequest;

/**
 * @author Nicholas Williams
 */
public class HeaderTimeZoneResolverTests extends TestCase {

	private static final TimeZone SYSTEM = TimeZone.getDefault();

	private static final String SYSTEM_ID = SYSTEM.getID();

	public void testResolveAmericaChicagoTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HeaderTimeZoneResolver.DEFAULT_HEADER_NAME, "America/Chicago");

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testResolveAmericaLosAngelesTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HeaderTimeZoneResolver.DEFAULT_HEADER_NAME, "America/Los_Angeles");

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithBadHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HeaderTimeZoneResolver.DEFAULT_HEADER_NAME, "America/Fake");

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithBadHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(HeaderTimeZoneResolver.DEFAULT_HEADER_NAME, "America/Fake");

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testResolveAmericaChicagoTimeZoneWithCustomHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo1", "America/Chicago");

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		resolver.setHeaderName("foo1");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testResolveLosAngelesTimeZoneWithCustomHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar2", "America/Los_Angeles");

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		resolver.setHeaderName("bar2");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithBadCustomHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("foo1", "America/Fake");

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		resolver.setHeaderName("foo1");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithBadCustomHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("bar2", "America/Fake");

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		resolver.setHeaderName("bar2");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithNoHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithNoHeader() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testCantSetTimeZone() {
		HeaderTimeZoneResolver resolver = new HeaderTimeZoneResolver();
		try {
			resolver.setTimeZone(null, null, null);
			fail("You should not be able to set the time zone on the header resolver.");
		} catch (UnsupportedOperationException e) {
			// good
		}
	}

}
