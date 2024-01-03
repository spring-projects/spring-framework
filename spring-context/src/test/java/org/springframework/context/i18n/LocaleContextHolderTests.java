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
	void testSetLocaleContext() {
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
	void testSetTimeZoneAwareLocaleContext() {
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
	void testSetLocale() {
		LocaleContextHolder.setLocale(Locale.GERMAN);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		boolean condition1 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition1).isFalse();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);

		LocaleContextHolder.setLocale(Locale.GERMANY);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		boolean condition = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isFalse();
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
	void testSetTimeZone() {
		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
		boolean condition1 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition1).isTrue();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
		boolean condition = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
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
	void testSetLocaleAndSetTimeZoneMixed() {
		LocaleContextHolder.setLocale(Locale.GERMANY);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		boolean condition5 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition5).isFalse();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
		boolean condition3 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition3).isTrue();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMANY);
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		LocaleContextHolder.setLocale(Locale.GERMAN);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));
		boolean condition2 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition2).isTrue();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+1"));

		LocaleContextHolder.setTimeZone(null);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		boolean condition4 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition4).isFalse();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
		boolean condition1 = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition1).isTrue();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isEqualTo(Locale.GERMAN);
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

		LocaleContextHolder.setLocale(null);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));
		boolean condition = LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext;
		assertThat(condition).isTrue();
		assertThat(LocaleContextHolder.getLocaleContext().getLocale()).isNull();
		assertThat(((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone()).isEqualTo(TimeZone.getTimeZone("GMT+2"));

		LocaleContextHolder.setTimeZone(null);
		assertThat(LocaleContextHolder.getLocale()).isEqualTo(Locale.getDefault());
		assertThat(LocaleContextHolder.getTimeZone()).isEqualTo(TimeZone.getDefault());
		assertThat(LocaleContextHolder.getLocaleContext()).isNull();
	}

}
