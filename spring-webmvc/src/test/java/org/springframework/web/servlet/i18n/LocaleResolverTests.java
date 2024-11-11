/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.web.servlet.LocaleContextResolver;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.testfixture.servlet.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Tests for various {@link LocaleResolver} implementations.
 *
 * @author Juergen Hoeller
 * @since 20.03.2003
 */
class LocaleResolverTests {

	@Test
	void acceptHeaderLocaleResolver() {
		doTest(new AcceptHeaderLocaleResolver(), false);
	}

	@Test
	void fixedLocaleResolver() {
		doTest(new FixedLocaleResolver(Locale.UK), false);
	}

	@Test
	void cookieLocaleResolver() {
		doTest(new CookieLocaleResolver(), true);
	}

	@Test
	void sessionLocaleResolver() {
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
		assertThat(locale).isEqualTo(Locale.UK);
		// set new locale
		try {
			localeResolver.setLocale(request, response, Locale.GERMANY);
			assertThat(shouldSet).as("should not be able to set Locale").isTrue();
			// check new locale
			locale = localeResolver.resolveLocale(request);
			assertThat(locale).isEqualTo(Locale.GERMANY);
		}
		catch (UnsupportedOperationException ex) {
			assertThat(shouldSet).as("should be able to set Locale").isFalse();
		}

		// check LocaleContext
		if (localeResolver instanceof LocaleContextResolver localeContextResolver) {
			LocaleContext localeContext = localeContextResolver.resolveLocaleContext(request);
			if (shouldSet) {
				assertThat(localeContext.getLocale()).isEqualTo(Locale.GERMANY);
			}
			else {
				assertThat(localeContext.getLocale()).isEqualTo(Locale.UK);
			}
			boolean condition2 = localeContext instanceof TimeZoneAwareLocaleContext;
			assertThat(condition2).isTrue();
			assertThat(((TimeZoneAwareLocaleContext) localeContext).getTimeZone()).isNull();

			if (localeContextResolver instanceof AbstractLocaleContextResolver) {
				((AbstractLocaleContextResolver) localeContextResolver).setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
				request.removeAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
				localeContextResolver.resolveLocaleContext(request);
				assertThat(TimeZone.getTimeZone("GMT+1")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());
			}

			try {
				localeContextResolver.setLocaleContext(request, response, new SimpleLocaleContext(Locale.US));
				if (!shouldSet) {
					fail("should not be able to set Locale");
				}
				localeContext = localeContextResolver.resolveLocaleContext(request);
				assertThat(localeContext.getLocale()).isEqualTo(Locale.US);
				if (localeContextResolver instanceof AbstractLocaleContextResolver) {
					assertThat(TimeZone.getTimeZone("GMT+1")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());
				}
				else {
					assertThat(((TimeZoneAwareLocaleContext) localeContext).getTimeZone()).isNull();
				}

				localeContextResolver.setLocaleContext(request, response,
						new SimpleTimeZoneAwareLocaleContext(Locale.GERMANY, TimeZone.getTimeZone("GMT+2")));
				localeContext = localeContextResolver.resolveLocaleContext(request);
				assertThat(localeContext.getLocale()).isEqualTo(Locale.GERMANY);
				boolean condition1 = localeContext instanceof TimeZoneAwareLocaleContext;
				assertThat(condition1).isTrue();
				assertThat(TimeZone.getTimeZone("GMT+2")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());

				localeContextResolver.setLocaleContext(request, response,
						new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+3")));
				localeContext = localeContextResolver.resolveLocaleContext(request);
				assertThat(localeContext.getLocale()).isEqualTo(Locale.UK);
				boolean condition = localeContext instanceof TimeZoneAwareLocaleContext;
				assertThat(condition).isTrue();
				assertThat(TimeZone.getTimeZone("GMT+3")).isEqualTo(((TimeZoneAwareLocaleContext) localeContext).getTimeZone());

				if (localeContextResolver instanceof AbstractLocaleContextResolver) {
					((AbstractLocaleContextResolver) localeContextResolver).setDefaultLocale(Locale.GERMANY);
					request.removeAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
					localeContextResolver.resolveLocaleContext(request);
					assertThat(localeContext.getLocale()).isEqualTo(Locale.GERMANY);
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
