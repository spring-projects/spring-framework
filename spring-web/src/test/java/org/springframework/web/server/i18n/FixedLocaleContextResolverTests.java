/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.server.i18n;

import java.time.ZoneOffset;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.testfixture.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.testfixture.server.MockServerWebExchange;

import static java.util.Locale.CANADA;
import static java.util.Locale.FRANCE;
import static java.util.Locale.US;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FixedLocaleContextResolver}.
 *
 * @author Sebastien Deleuze
 */
class FixedLocaleContextResolverTests {

	@BeforeEach
	void setup() {
		Locale.setDefault(US);
	}

	@Test
	void resolveDefaultLocale() {
		FixedLocaleContextResolver resolver = new FixedLocaleContextResolver();
		assertThat(resolver.resolveLocaleContext(exchange()).getLocale()).isEqualTo(US);
		assertThat(resolver.resolveLocaleContext(exchange(CANADA)).getLocale()).isEqualTo(US);
	}

	@Test
	void resolveCustomizedLocale() {
		FixedLocaleContextResolver resolver = new FixedLocaleContextResolver(FRANCE);
		assertThat(resolver.resolveLocaleContext(exchange()).getLocale()).isEqualTo(FRANCE);
		assertThat(resolver.resolveLocaleContext(exchange(CANADA)).getLocale()).isEqualTo(FRANCE);
	}

	@Test
	void resolveCustomizedAndTimeZoneLocale() {
		TimeZone timeZone = TimeZone.getTimeZone(ZoneOffset.UTC);
		FixedLocaleContextResolver resolver = new FixedLocaleContextResolver(FRANCE, timeZone);
		TimeZoneAwareLocaleContext context = (TimeZoneAwareLocaleContext) resolver.resolveLocaleContext(exchange());
		assertThat(context.getLocale()).isEqualTo(FRANCE);
		assertThat(context.getTimeZone()).isEqualTo(timeZone);
	}

	private ServerWebExchange exchange(Locale... locales) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("").acceptLanguageAsLocales(locales));
	}

}
