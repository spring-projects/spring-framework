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
import org.springframework.mock.web.test.MockHttpServletResponse;

/**
 * @author Nicholas Williams
 */
public class SessionTimeZoneResolverTests extends TestCase {

	private static final TimeZone SYSTEM = TimeZone.getDefault();

	private static final String SYSTEM_ID = SYSTEM.getID();

	public void testResolveAmericaChicagoTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession().setAttribute(
				SessionTimeZoneResolver.TIME_ZONE_SESSION_ATTRIBUTE_NAME,
				TimeZone.getTimeZone("America/Chicago")
		);

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testResolveAmericaLosAngelesTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession().setAttribute(
				SessionTimeZoneResolver.TIME_ZONE_SESSION_ATTRIBUTE_NAME,
				TimeZone.getTimeZone("America/Los_Angeles")
		);

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithoutAttribute() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession().setAttribute("foo", "bar");

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithoutAttribute() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.getSession().setAttribute("foo", "bar");

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithoutSession() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithoutSession() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testSetAndResolveAmericaChicagoTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Chicago");

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testSetAndResolveAmericaLosAngelesTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Los_Angeles");

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testSetToChicagoThenNullResolveSystemDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Chicago");

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);
		resolver.setTimeZone(request, response, null);

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testSetToLosAngelesThenNullResolveSystemDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Los_Angeles");

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);
		resolver.setTimeZone(request, response, null);

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testSetToChicagoThenNullResolveSetDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Chicago");

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("America/New_York"));
		resolver.setTimeZone(request, response, setZone);
		resolver.setTimeZone(request, response, null);

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/New_York", timeZone.getID());
	}

	public void testSetToLosAngelesThenNullResolveSetDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Los_Angeles");

		SessionTimeZoneResolver resolver = new SessionTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("America/Denver"));
		resolver.setTimeZone(request, response, setZone);
		resolver.setTimeZone(request, response, null);

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Denver", timeZone.getID());
	}

}
