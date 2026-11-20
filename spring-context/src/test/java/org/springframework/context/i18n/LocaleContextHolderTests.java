/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 */
class LocaleContextHolderTests {

	@Test
	void setLocaleContext() {
		LocaleContext lc = new SimpleLocaleContext(Locale.GERMAN);
		LocaleContextHolder.setLocaleContext(lc);
		assertThat(LocaleContextHolder.getLocaleContext()).isSameAs(lc);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

		lc = new SimpleLocaleContext(Locale.GERMANY);
		LocaleContextHolder.setLocaleContext(lc);
		assertThat(LocaleContextHolder.getLocaleContext()).isSameAs(lc);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

		LocaleContextHolder.resetLocaleContext();
		assertThat(LocaleContextHolder.getLocaleContext()).isNull();
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
	}

	@Test
	void setTimeZoneAwareLocaleContext() {
		LocaleContext lc = new SimpleTimeZoneAwareLocaleContext(Locale.GERMANY, TimeZone.getTimeZone("GMT+1"));
		LocaleContextHolder.setLocaleContext(lc);
		assertThat(LocaleContextHolder.getLocaleContext()).isSameAs(lc);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		LocaleContextHolder.resetLocaleContext();
		assertThat(LocaleContextHolder.getLocaleContext()).isNull();
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
	}

	@Test
	void setLocale() {
		LocaleContextHolder.setLocale(Locale.GERMAN);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		assertThat(LocaleContextHolder.getLocaleContext()).isNotInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);

		LocaleContextHolder.setLocale(Locale.GERMANY);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		assertThat(LocaleContextHolder.getLocaleContext()).isNotInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);

		LocaleContextHolder.setLocale(null);
		assertThat(LocaleContextHolder.getLocaleContext()).isNull();
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

		LocaleContextHolder.setDefaultLocale(Locale.GERMAN);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		LocaleContextHolder.setDefaultLocale(null);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
	}

	@Test
	void setTimeZone() {
		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getLocaleContext()).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
		assertThat(LocaleContextHolder.getLocaleContext()).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

		LocaleContextHolder.setTimeZone(null);
		assertThat(LocaleContextHolder.getLocaleContext()).isNull();
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());

		LocaleContextHolder.setDefaultTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
		LocaleContextHolder.setDefaultTimeZone(null);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
	}

	@Test
	void setLocaleAndSetTimeZoneMixed() {
		LocaleContextHolder.setLocale(Locale.GERMANY);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		assertThat(LocaleContextHolder.getLocaleContext()).isNotInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getLocaleContext()).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		LocaleContextHolder.setLocale(Locale.GERMAN);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getLocaleContext()).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		LocaleContextHolder.setTimeZone(null);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		assertThat(LocaleContextHolder.getLocaleContext()).isNotInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
		assertThat(LocaleContextHolder.getLocaleContext()).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

		LocaleContextHolder.setLocale(null);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
		assertThat(LocaleContextHolder.getLocaleContext()).isInstanceOf(TimeZoneAwareLocaleContext.class);
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

		LocaleContextHolder.setTimeZone(null);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		assertThat(LocaleContextHolder.getLocaleContext()).isNull();
	}

}
