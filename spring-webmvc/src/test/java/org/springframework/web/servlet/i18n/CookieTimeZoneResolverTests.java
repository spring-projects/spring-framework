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

import javax.servlet.http.Cookie;

import junit.framework.TestCase;

import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;

/**
 * @author Nicholas Williams
 */
public class CookieTimeZoneResolverTests extends TestCase {

	private static final TimeZone SYSTEM = TimeZone.getDefault();

	private static final String SYSTEM_ID = SYSTEM.getID();

	public void testResolveAmericaChicagoTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME, "America/Chicago"));

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());

		request.setCookies();
		assertEquals("The cookies are not right.", 0, request.getCookies().length);

		timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testResolveAmericaLosAngelesTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME, "America/Los_Angeles"));

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());

		request.setCookies();
		assertEquals("The cookies are not right.", 0, request.getCookies().length);

		timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithBadCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME, "America/Fake"));

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithBadCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME, "America/Fake"));

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testResolveAmericaChicagoTimeZoneWithCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("foo1", "America/Chicago"));

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setCookieName("foo1");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());

		request.setCookies();
		assertEquals("The cookies are not right.", 0, request.getCookies().length);

		timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testResolveAmericaLosAngelesTimeZoneWithCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("bar2", "America/Los_Angeles"));

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setCookieName("bar2");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());

		request.setCookies();
		assertEquals("The cookies are not right.", 0, request.getCookies().length);

		timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithBadCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("foo1", "America/Fake"));

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setCookieName("foo1");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithBadCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("bar2", "America/Fake"));

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		resolver.setCookieName("bar2");
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testResolveSystemDefaultTimeZoneWithNoCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveSetDefaultTimeZoneWithNoCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		String targetId = SYSTEM_ID.equals("America/Chicago") ? "America/Los_Angeles" : "America/Chicago";

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone(targetId));
		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", targetId, timeZone.getID());
	}

	public void testSetAndResolveAmericaChicagoTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Chicago");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Chicago", cookie.getValue());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testSetAndResolveAmericaLosAngelesTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Los_Angeles");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Los_Angeles", cookie.getValue());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testSetAndResolveAmericaChicagoTimeZoneWithCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Chicago");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setCookieName("foo1");
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie("foo1");
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Chicago", cookie.getValue());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testSetAndResolveAmericaLosAngelesTimeZoneWithCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Los_Angeles");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setCookieName("bar2");
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie("bar2");
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Los_Angeles", cookie.getValue());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testSetToChicagoThenNullResolveSystemDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Chicago");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Chicago", cookie.getValue());

		response = new MockHttpServletResponse();
		resolver.setTimeZone(request, response, null);

		cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "", cookie.getValue());
		assertEquals("The cookie has the wrong max age.", 0, cookie.getMaxAge());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testSetToLosAngelesThenNullResolveSystemDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Los_Angeles");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Los_Angeles", cookie.getValue());

		response = new MockHttpServletResponse();
		resolver.setTimeZone(request, response, null);

		cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "", cookie.getValue());
		assertEquals("The cookie has the wrong max age.", 0, cookie.getMaxAge());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testSetToChicagoThenNullResolveSetDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Chicago");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("America/New_York"));
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Chicago", cookie.getValue());

		response = new MockHttpServletResponse();
		resolver.setTimeZone(request, response, null);

		cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "", cookie.getValue());
		assertEquals("The cookie has the wrong max age.", 0, cookie.getMaxAge());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/New_York", timeZone.getID());
	}

	public void testSetToLosAngelesThenNullResolveSetDefaultTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		TimeZone setZone = TimeZone.getTimeZone("America/Los_Angeles");

		CookieTimeZoneResolver resolver = new CookieTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("America/Denver"));
		resolver.setTimeZone(request, response, setZone);

		Cookie cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "America/Los_Angeles", cookie.getValue());

		response = new MockHttpServletResponse();
		resolver.setTimeZone(request, response, null);

		cookie = response.getCookie(CookieTimeZoneResolver.DEFAULT_COOKIE_NAME);
		assertNotNull("The cookie should not be null.", cookie);
		assertEquals("The cookie value is not correct.", "", cookie.getValue());
		assertEquals("The cookie has the wrong max age.", 0, cookie.getMaxAge());

		TimeZone timeZone = resolver.resolveTimeZone(request);
		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Denver", timeZone.getID());
	}

}
