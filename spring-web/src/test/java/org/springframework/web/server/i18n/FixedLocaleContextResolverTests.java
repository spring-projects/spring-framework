package org.springframework.web.server.i18n;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.i18n.TimeZoneAwareLocaleContext;
import org.springframework.mock.http.server.reactive.test.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.test.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.time.ZoneId;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Locale.*;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link FixedLocaleContextResolver}.
 *
 * @author Sebastien Deleuze
 */
public class FixedLocaleContextResolverTests {

	private FixedLocaleContextResolver resolver;

	@Before
	public void setup() {
		Locale.setDefault(US);
	}

	@Test
	public void resolveDefaultLocale() {
		this.resolver = new FixedLocaleContextResolver();
		assertEquals(US, this.resolver.resolveLocaleContext(exchange()).getLocale());
		assertEquals(US, this.resolver.resolveLocaleContext(exchange(CANADA)).getLocale());
	}

	@Test
	public void resolveCustomizedLocale() {
		this.resolver = new FixedLocaleContextResolver(FRANCE);
		assertEquals(FRANCE, this.resolver.resolveLocaleContext(exchange()).getLocale());
		assertEquals(FRANCE, this.resolver.resolveLocaleContext(exchange(CANADA)).getLocale());
	}

	@Test
	public void resolveCustomizedAndTimeZoneLocale() {
		TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of("UTC"));
		this.resolver = new FixedLocaleContextResolver(FRANCE, timeZone);
		TimeZoneAwareLocaleContext context = (TimeZoneAwareLocaleContext)this.resolver.resolveLocaleContext(exchange());
		assertEquals(FRANCE, context.getLocale());
		assertEquals(timeZone, context.getTimeZone());
	}

	private ServerWebExchange exchange(Locale... locales) {
		return new MockServerWebExchange(MockServerHttpRequest
				.get("")
				.acceptLanguageAsLocales(locales)
				.build());
	}

}
