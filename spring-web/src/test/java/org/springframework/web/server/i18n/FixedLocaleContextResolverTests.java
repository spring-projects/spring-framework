/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.server.i18n;

import java.time.ZoneId;
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
		TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of("UTC"));
		FixedLocaleContextResolver resolver = new FixedLocaleContextResolver(FRANCE, timeZone);
		TimeZoneAwareLocaleContext context = (TimeZoneAwareLocaleContext) resolver.resolveLocaleContext(exchange());
		assertThat(context.getLocale()).isEqualTo(FRANCE);
		assertThat(context.getTimeZone()).isEqualTo(timeZone);
	}

	private ServerWebExchange exchange(Locale... locales) {
		return MockServerWebExchange.from(MockServerHttpRequest.get("").acceptLanguageAsLocales(locales));
	}

}
