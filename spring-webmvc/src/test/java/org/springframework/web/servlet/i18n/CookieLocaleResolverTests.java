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

package org.springframework.web.servlet.i18n;

import java.util.Locale;
import java.util.TimeZone;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.Test;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.junit.Assert.*;

/**
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rick Evans
 */
public class CookieLocaleResolverTests {

	@Test
	public void testResolveLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		Locale loc = resolver.resolveLocale(request);
		assertEquals("nl", loc.getLanguage());
	}

	@Test
	public void testResolveLocaleContext() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals("nl", loc.getLocale().getLanguage());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testResolveLocaleContextWithTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl GMT+1");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals("nl", loc.getLocale().getLanguage());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertEquals(TimeZone.getTimeZone("GMT+1"), ((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testResolveLocaleContextWithInvalidLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "n-x GMT+1");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		try {
			resolver.resolveLocaleContext(request);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("LanguageKoekje"));
			assertTrue(ex.getMessage().contains("n-x GMT+1"));
		}
	}

	@Test
	public void testResolveLocaleContextWithInvalidLocaleOnErrorDispatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.GERMAN);
		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
		Cookie cookie = new Cookie("LanguageKoekje", "n-x GMT+1");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals(Locale.GERMAN, loc.getLocale());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertEquals(TimeZone.getTimeZone("GMT+2"), ((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testResolveLocaleContextWithInvalidTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoekje");
		try {
			resolver.resolveLocaleContext(request);
			fail("Should have thrown IllegalStateException");
		}
		catch (IllegalStateException ex) {
			assertTrue(ex.getMessage().contains("LanguageKoekje"));
			assertTrue(ex.getMessage().contains("nl X-MT"));
		}
	}

	@Test
	public void testResolveLocaleContextWithInvalidTimeZoneOnErrorDispatch() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
		Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
		resolver.setCookieName("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals("nl", loc.getLocale().getLanguage());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertEquals(TimeZone.getTimeZone("GMT+2"), ((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testSetAndResolveLocale() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocale(request, response, new Locale("nl", ""));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertNotNull(cookie);
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_NAME, cookie.getName());
		assertEquals(null, cookie.getDomain());
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_PATH, cookie.getPath());
		assertFalse(cookie.getSecure());

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertEquals("nl", loc.getLanguage());
	}

	@Test
	public void testSetAndResolveLocaleContext() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response, new SimpleLocaleContext(new Locale("nl", "")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals("nl", loc.getLocale().getLanguage());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testSetAndResolveLocaleContextWithTimeZone() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response,
				new SimpleTimeZoneAwareLocaleContext(new Locale("nl", ""), TimeZone.getTimeZone("GMT+1")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals("nl", loc.getLocale().getLanguage());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertEquals(TimeZone.getTimeZone("GMT+1"), ((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testSetAndResolveLocaleContextWithTimeZoneOnly() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response,
				new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+1")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.GERMANY);
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals(Locale.GERMANY, loc.getLocale());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertEquals(TimeZone.getTimeZone("GMT+1"), ((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testSetAndResolveLocaleWithCountry() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocale(request, response, new Locale("de", "AT"));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertNotNull(cookie);
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_NAME, cookie.getName());
		assertEquals(null, cookie.getDomain());
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_PATH, cookie.getPath());
		assertFalse(cookie.getSecure());
		assertEquals("de_AT", cookie.getValue());

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertEquals("de", loc.getLanguage());
		assertEquals("AT", loc.getCountry());
	}

	@Test
	public void testSetAndResolveLocaleWithCountryAsLanguageTag() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLanguageTagCompliant(true);
		resolver.setLocale(request, response, new Locale("de", "AT"));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertNotNull(cookie);
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_NAME, cookie.getName());
		assertEquals(null, cookie.getDomain());
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_PATH, cookie.getPath());
		assertFalse(cookie.getSecure());
		assertEquals("de-AT", cookie.getValue());

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		resolver.setLanguageTagCompliant(true);
		Locale loc = resolver.resolveLocale(request);
		assertEquals("de", loc.getLanguage());
		assertEquals("AT", loc.getCountry());
	}

	@Test
	public void testCustomCookie() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoek");
		resolver.setCookieDomain(".springframework.org");
		resolver.setCookiePath("/mypath");
		resolver.setCookieMaxAge(10000);
		resolver.setCookieSecure(true);
		resolver.setLocale(request, response, new Locale("nl", ""));

		Cookie cookie = response.getCookie("LanguageKoek");
		assertNotNull(cookie);
		assertEquals("LanguageKoek", cookie.getName());
		assertEquals(".springframework.org", cookie.getDomain());
		assertEquals("/mypath", cookie.getPath());
		assertEquals(10000, cookie.getMaxAge());
		assertTrue(cookie.getSecure());

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		resolver.setCookieName("LanguageKoek");
		Locale loc = resolver.resolveLocale(request);
		assertEquals("nl", loc.getLanguage());
	}

	@Test
	public void testResolveLocaleWithoutCookie() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		Locale loc = resolver.resolveLocale(request);
		assertEquals(request.getLocale(), loc);
	}

	@Test
	public void testResolveLocaleContextWithoutCookie() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals(request.getLocale(), loc.getLocale());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testResolveLocaleWithoutCookieAndDefaultLocale() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.GERMAN);

		Locale loc = resolver.resolveLocale(request);
		assertEquals(Locale.GERMAN, loc);
	}

	@Test
	public void testResolveLocaleContextWithoutCookieAndDefaultLocale() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.GERMAN);
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals(Locale.GERMAN, loc.getLocale());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertEquals(TimeZone.getTimeZone("GMT+1"), ((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testResolveLocaleWithCookieWithoutLocale() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		Locale loc = resolver.resolveLocale(request);
		assertEquals(request.getLocale(), loc);
	}

	@Test
	public void testResolveLocaleContextWithCookieWithoutLocale() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
		request.setCookies(cookie);

		CookieLocaleResolver resolver = new CookieLocaleResolver();

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertEquals(request.getLocale(), loc.getLocale());
		assertTrue(loc instanceof TimeZoneAwareLocaleContext);
		assertNull(((TimeZoneAwareLocaleContext) loc).getTimeZone());
	}

	@Test
	public void testSetLocaleToNull() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertEquals(Locale.TAIWAN, locale);

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_NAME, localeCookie.getName());
		assertEquals("", localeCookie.getValue());
	}

	@Test
	public void testSetLocaleContextToNull() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setLocaleContext(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertEquals(Locale.TAIWAN, locale);
		TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
		assertNull(timeZone);

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_NAME, localeCookie.getName());
		assertEquals("", localeCookie.getValue());
	}

	@Test
	public void testSetLocaleToNullWithDefault() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.CANADA_FRENCH);
		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertEquals(Locale.CANADA_FRENCH, locale);

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_NAME, localeCookie.getName());
		assertEquals("", localeCookie.getValue());
	}

	@Test
	public void testSetLocaleContextToNullWithDefault() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);
		MockHttpServletResponse response = new MockHttpServletResponse();

		CookieLocaleResolver resolver = new CookieLocaleResolver();
		resolver.setDefaultLocale(Locale.CANADA_FRENCH);
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
		resolver.setLocaleContext(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertEquals(Locale.CANADA_FRENCH, locale);
		TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
		assertEquals(TimeZone.getTimeZone("GMT+1"), timeZone);

		Cookie[] cookies = response.getCookies();
		assertEquals(1, cookies.length);
		Cookie localeCookie = cookies[0];
		assertEquals(CookieLocaleResolver.DEFAULT_COOKIE_NAME, localeCookie.getName());
		assertEquals("", localeCookie.getValue());
	}

}
