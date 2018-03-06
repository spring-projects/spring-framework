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

import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.mock.web.test.MockHttpServletRequest;
import org.springframework.mock.web.test.MockHttpServletResponse;
import org.springframework.mock.web.test.MockServletContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 20.03.2003
 */
public class LocaleResolverTests {

	@Test
	public void testAcceptHeaderLocaleResolver() {
		doTest(new AcceptHeaderLocaleResolver(), false);
	}

	@Test
	public void testFixedLocaleResolver() {
		doTest(new FixedLocaleResolver(Locale.UK), false);
	}

	@Test
	public void testCookieLocaleResolver() {
		doTest(new CookieLocaleResolver(), true);
	}

	@Test
	public void testSessionLocaleResolver() {
		doTest(new SessionLocaleResolver(), true);
	}

	private void doTest(LocaleResolver localeResolver, boolean shouldSet) {
		// create mocks
		MockServletContext context = new MockServletContext();
		MockHttpServletRequest request = new MockHttpServletRequest(context);
		request.addPreferredLocale(Locale.UK);
		MockHttpServletResponse response = new MockHttpServletResponse();

		// check original locale
		Locale locale = localeResolver.resolveLocale(request);
		assertEquals(Locale.UK, locale);
		// set new locale
		try {
			localeResolver.setLocale(request, response, Locale.GERMANY);
			if (!shouldSet)
				fail("should not be able to set Locale");
			// check new locale
			locale = localeResolver.resolveLocale(request);
			assertEquals(Locale.GERMANY, locale);
		}
		catch (UnsupportedOperationException ex) {
			if (shouldSet) {
				fail("should be able to set Locale");
			}
		}

		// check LocaleContext
		if (localeResolver instanceof LocaleContextResolver) {
			LocaleContextResolver localeContextResolver = (LocaleContextResolver) localeResolver;
			LocaleContext localeContext = localeContextResolver.resolveLocaleContext(request);
			if (shouldSet) {
				assertEquals(Locale.GERMANY, localeContext.getLocale());
			}
			else {
				assertEquals(Locale.UK, localeContext.getLocale());
			}
			assertTrue(localeContext instanceof TimeZoneAwareLocaleContext);
			assertNull(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());

			if (localeContextResolver instanceof AbstractLocaleContextResolver) {
				((AbstractLocaleContextResolver) localeContextResolver).setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
				assertEquals(((TimeZoneAwareLocaleContext) localeContext).getTimeZone(), TimeZone.getTimeZone("GMT+1"));
			}

			try {
				localeContextResolver.setLocaleContext(request, response, new SimpleLocaleContext(Locale.US));
				if (!shouldSet) {
					fail("should not be able to set Locale");
				}
				localeContext = localeContextResolver.resolveLocaleContext(request);
				assertEquals(Locale.US, localeContext.getLocale());
				if (localeContextResolver instanceof AbstractLocaleContextResolver) {
					assertEquals(((TimeZoneAwareLocaleContext) localeContext).getTimeZone(), TimeZone.getTimeZone("GMT+1"));
				}
				else {
					assertNull(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());
				}

				localeContextResolver.setLocaleContext(request, response,
						new SimpleTimeZoneAwareLocaleContext(Locale.GERMANY, TimeZone.getTimeZone("GMT+2")));
				localeContext = localeContextResolver.resolveLocaleContext(request);
				assertEquals(Locale.GERMANY, localeContext.getLocale());
				assertTrue(localeContext instanceof TimeZoneAwareLocaleContext);
				assertEquals(((TimeZoneAwareLocaleContext) localeContext).getTimeZone(), TimeZone.getTimeZone("GMT+2"));

				localeContextResolver.setLocaleContext(request, response,
						new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+3")));
				localeContext = localeContextResolver.resolveLocaleContext(request);
				assertEquals(Locale.UK, localeContext.getLocale());
				assertTrue(localeContext instanceof TimeZoneAwareLocaleContext);
				assertEquals(((TimeZoneAwareLocaleContext) localeContext).getTimeZone(), TimeZone.getTimeZone("GMT+3"));

				if (localeContextResolver instanceof AbstractLocaleContextResolver) {
					((AbstractLocaleContextResolver) localeContextResolver).setDefaultLocale(Locale.GERMANY);
					assertEquals(Locale.GERMANY, localeContext.getLocale());
				}
			}
			catch (UnsupportedOperationException ex) {
				if (shouldSet) {
					fail("should be able to set Locale");
				}
			}
		}
	}

}
