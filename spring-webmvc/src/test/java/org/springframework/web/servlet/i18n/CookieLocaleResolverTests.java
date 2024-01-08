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

import java.time.Duration;
import java.util.Locale;
import java.util.TimeZone;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.LocaleContext;
import org.springframework.context.i18n.SimpleLocaleContext;
import org.springframework.context.i18n.SimpleTimeZoneAwareLocaleContext;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.testfixture.servlet.MockCookie;
import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;
import org.springframework.web.util.WebUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link CookieLocaleResolver}.
 *
 * @author Alef Arendsen
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Sam Brannen
 * @author Vedran Pavic
 */
class CookieLocaleResolverTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private CookieLocaleResolver resolver = new CookieLocaleResolver();


	@Test
	void resolveLocale() {
		Cookie cookie = new Cookie("LanguageKoekje", "nl");
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoekje");
		Locale loc = resolver.resolveLocale(request);
		assertThat(loc.getLanguage()).isEqualTo("nl");
	}

	@Test
	void resolveLocaleContext() {
		Cookie cookie = new Cookie("LanguageKoekje", "nl");
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
	}

	@Test
	void resolveLocaleContextWithTimeZone() {
		Cookie cookie = new Cookie("LanguageKoekje", "nl GMT+1");
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoekje");
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	void resolveLocaleContextWithInvalidLocale() {
		Cookie cookie = new Cookie("LanguageKoekje", "++ GMT+1");
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoekje");
		assertThatIllegalStateException().isThrownBy(() -> resolver.resolveLocaleContext(request))
			.withMessageContaining("LanguageKoekje")
			.withMessageContaining("++ GMT+1");
	}

	@Test
	void resolveLocaleContextWithInvalidLocaleOnErrorDispatch() {
		request.addPreferredLocale(Locale.GERMAN);
		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
		Cookie cookie = new Cookie("LanguageKoekje", "++ GMT+1");
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoekje");
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
	}

	@Test
	void resolveLocaleContextWithInvalidTimeZone() {
		Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoekje");
		assertThatIllegalStateException().isThrownBy(() -> resolver.resolveLocaleContext(request))
			.withMessageContaining("LanguageKoekje")
			.withMessageContaining("nl X-MT");
	}

	@Test
	void resolveLocaleContextWithInvalidTimeZoneOnErrorDispatch() {
		request.setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, new ServletException());
		Cookie cookie = new Cookie("LanguageKoekje", "nl X-MT");
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoekje");
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+2"));
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
	}

	@Test
	void setAndResolveLocale() {
		resolver.setLocale(request, response, new Locale("nl", ""));

		MockCookie cookie = MockCookie.parse(response.getHeader(HttpHeaders.SET_COOKIE));
		assertThat(cookie).isNotNull();
		assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat(cookie.getDomain()).isNull();
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isFalse();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertThat(loc.getLanguage()).isEqualTo("nl");
	}

	@Test
	void setAndResolveLocaleContext() {
		resolver.setLocaleContext(request, response, new SimpleLocaleContext(new Locale("nl", "")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
	}

	@Test
	void setAndResolveLocaleContextWithTimeZone() {
		resolver.setLocaleContext(request, response,
				new SimpleTimeZoneAwareLocaleContext(new Locale("nl", ""), TimeZone.getTimeZone("GMT+1")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale().getLanguage()).isEqualTo("nl");
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	void setAndResolveLocaleContextWithTimeZoneOnly() {
		resolver.setLocaleContext(request, response,
				new SimpleTimeZoneAwareLocaleContext(null, TimeZone.getTimeZone("GMT+1")));

		Cookie cookie = response.getCookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		request = new MockHttpServletRequest();
		request.addPreferredLocale(Locale.GERMANY);
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	void setAndResolveLocaleWithCountry() {
		resolver.setLocale(request, response, new Locale("de", "AT"));

		MockCookie cookie = MockCookie.parse(response.getHeader(HttpHeaders.SET_COOKIE));
		assertThat(cookie).isNotNull();
		assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat(cookie.getDomain()).isNull();
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isFalse();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getValue()).isEqualTo("de-AT");

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertThat(loc.getLanguage()).isEqualTo("de");
		assertThat(loc.getCountry()).isEqualTo("AT");
	}

	@Test
	void setAndResolveLocaleWithCountryAsLegacyJava() {
		resolver.setLanguageTagCompliant(false);
		resolver.setLocale(request, response, new Locale("de", "AT"));

		MockCookie cookie = MockCookie.parse(response.getHeader(HttpHeaders.SET_COOKIE));
		assertThat(cookie).isNotNull();
		assertThat(cookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat(cookie.getDomain()).isNull();
		assertThat(cookie.getPath()).isEqualTo("/");
		assertThat(cookie.getSecure()).isFalse();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");
		assertThat(cookie.getValue()).isEqualTo("de_AT");

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver();
		Locale loc = resolver.resolveLocale(request);
		assertThat(loc.getLanguage()).isEqualTo("de");
		assertThat(loc.getCountry()).isEqualTo("AT");
	}

	@Test
	void customCookie() {
		resolver = new CookieLocaleResolver("LanguageKoek");
		resolver.setCookieDomain(".springframework.org");
		resolver.setCookiePath("/mypath");
		resolver.setCookieMaxAge(Duration.ofSeconds(10000));
		resolver.setCookieSecure(true);
		resolver.setCookieSameSite("Lax");
		resolver.setLocale(request, response, new Locale("nl", ""));

		MockCookie cookie = MockCookie.parse(response.getHeader(HttpHeaders.SET_COOKIE));
		assertThat(cookie).isNotNull();
		assertThat(cookie.getName()).isEqualTo("LanguageKoek");
		assertThat(cookie.getDomain()).isEqualTo(".springframework.org");
		assertThat(cookie.getPath()).isEqualTo("/mypath");
		assertThat(cookie.getMaxAge()).isEqualTo(10000);
		assertThat(cookie.getSecure()).isTrue();
		assertThat(cookie.getSameSite()).isEqualTo("Lax");

		request = new MockHttpServletRequest();
		request.setCookies(cookie);

		resolver = new CookieLocaleResolver("LanguageKoek");
		Locale loc = resolver.resolveLocale(request);
		assertThat(loc.getLanguage()).isEqualTo("nl");
	}

	@Test
	void resolveLocaleWithoutCookie() {
		request.addPreferredLocale(Locale.TAIWAN);

		Locale loc = resolver.resolveLocale(request);
		assertThat(loc).isEqualTo(request.getLocale());
	}

	@Test
	void resolveLocaleContextWithoutCookie() {
		request.addPreferredLocale(Locale.TAIWAN);

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale()).isEqualTo(request.getLocale());
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
	}

	@Test
	void resolveLocaleWithoutCookieAndDefaultLocale() {
		request.addPreferredLocale(Locale.TAIWAN);

		resolver.setDefaultLocale(Locale.GERMAN);

		Locale loc = resolver.resolveLocale(request);
		assertThat(loc).isEqualTo(Locale.GERMAN);
	}

	@Test
	void resolveLocaleContextWithoutCookieAndDefaultLocale() {
		request.addPreferredLocale(Locale.TAIWAN);

		resolver.setDefaultLocale(Locale.GERMAN);
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

	@Test
	void resolveLocaleWithCookieWithoutLocale() {
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
		request.setCookies(cookie);

		Locale loc = resolver.resolveLocale(request);
		assertThat(loc).isEqualTo(request.getLocale());
	}

	@Test
	void resolveLocaleContextWithCookieWithoutLocale() {
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, "");
		request.setCookies(cookie);

		LocaleContext loc = resolver.resolveLocaleContext(request);
		assertThat(loc.getLocale()).isEqualTo(request.getLocale());
		assertThat(loc).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(((TimeZoneAwareLocaleContext) loc).getTimeZone()).isNull();
	}

	@Test
	void setLocaleToNull() {
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);

		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat(locale).isEqualTo(Locale.TAIWAN);

		Cookie[] cookies = response.getCookies();
		assertThat(cookies).hasSize(1);
		Cookie localeCookie = cookies[0];
		assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat(localeCookie.getValue()).isEmpty();
	}

	@Test
	void setLocaleContextToNull() {
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);

		resolver.setLocaleContext(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat(locale).isEqualTo(Locale.TAIWAN);
		TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
		assertThat(timeZone).isNull();

		Cookie[] cookies = response.getCookies();
		assertThat(cookies).hasSize(1);
		Cookie localeCookie = cookies[0];
		assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat(localeCookie.getValue()).isEmpty();
	}

	@Test
	void setLocaleToNullWithDefault() {
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);

		resolver.setDefaultLocale(Locale.CANADA_FRENCH);
		resolver.setLocale(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat(locale).isEqualTo(Locale.CANADA_FRENCH);

		Cookie[] cookies = response.getCookies();
		assertThat(cookies).hasSize(1);
		Cookie localeCookie = cookies[0];
		assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat(localeCookie.getValue()).isEmpty();
	}

	@Test
	void setLocaleContextToNullWithDefault() {
		request.addPreferredLocale(Locale.TAIWAN);
		Cookie cookie = new Cookie(CookieLocaleResolver.DEFAULT_COOKIE_NAME, Locale.UK.toString());
		request.setCookies(cookie);

		resolver.setDefaultLocale(Locale.CANADA_FRENCH);
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
		resolver.setLocaleContext(request, response, null);
		Locale locale = (Locale) request.getAttribute(CookieLocaleResolver.LOCALE_REQUEST_ATTRIBUTE_NAME);
		assertThat(locale).isEqualTo(Locale.CANADA_FRENCH);
		TimeZone timeZone = (TimeZone) request.getAttribute(CookieLocaleResolver.TIME_ZONE_REQUEST_ATTRIBUTE_NAME);
		assertThat(timeZone).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		Cookie[] cookies = response.getCookies();
		assertThat(cookies).hasSize(1);
		Cookie localeCookie = cookies[0];
		assertThat(localeCookie.getName()).isEqualTo(CookieLocaleResolver.DEFAULT_COOKIE_NAME);
		assertThat(localeCookie.getValue()).isEmpty();
	}

	@Test
	void customDefaultLocaleFunction() {
		request.addPreferredLocale(Locale.TAIWAN);

		resolver.setDefaultLocaleFunction(request -> Locale.GERMAN);

		assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.GERMAN);
	}

	@Test
	void customDefaultTimeZoneFunction() {
		request.addPreferredLocale(Locale.TAIWAN);

		resolver.setDefaultTimeZoneFunction(request -> TimeZone.getTimeZone("GMT+1"));

		TimeZoneAwareLocaleContext context = (TimeZoneAwareLocaleContext) resolver.resolveLocaleContext(request);
		assertThat(context.getLocale()).isEqualTo(Locale.TAIWAN);
		assertThat(context.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
	}

}
