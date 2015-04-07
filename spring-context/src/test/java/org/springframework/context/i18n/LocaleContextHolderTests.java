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

package org.springframework.context.i18n;

import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 */
public class LocaleContextHolderTests {

	@Test
	public void testSetLocaleContext() {
		LocaleContext lc = new SimpleLocaleContext(Locale.GERMAN);
		LocaleContextHolder.setLocaleContext(lc);
		assertSame(lc, LocaleContextHolder.getLocaleContext());
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());

		lc = new SimpleLocaleContext(Locale.GERMANY);
		LocaleContextHolder.setLocaleContext(lc);
		assertSame(lc, LocaleContextHolder.getLocaleContext());
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());

		LocaleContextHolder.resetLocaleContext();
		assertNull(LocaleContextHolder.getLocaleContext());
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
	}

	@Test
	public void testSetTimeZoneAwareLocaleContext() {
		LocaleContext lc = new SimpleTimeZoneAwareLocaleContext(Locale.GERMANY, TimeZone.getTimeZone("GMT+1"));
		LocaleContextHolder.setLocaleContext(lc);
		assertSame(lc, LocaleContextHolder.getLocaleContext());
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+1"), LocaleContextHolder.getTimeZone());

		LocaleContextHolder.resetLocaleContext();
		assertNull(LocaleContextHolder.getLocaleContext());
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
	}

	@Test
	public void testSetLocale() {
		LocaleContextHolder.setLocale(Locale.GERMAN);
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
		assertFalse(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocaleContext().getLocale());

		LocaleContextHolder.setLocale(Locale.GERMANY);
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
		assertFalse(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocaleContext().getLocale());

		LocaleContextHolder.setLocale(null);
		assertNull(LocaleContextHolder.getLocaleContext());
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
	}

	@Test
	public void testSetTimeZone() {
		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+1"), LocaleContextHolder.getTimeZone());
		assertTrue(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertNull(LocaleContextHolder.getLocaleContext().getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+1"), ((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone());

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+2"), LocaleContextHolder.getTimeZone());
		assertTrue(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertNull(LocaleContextHolder.getLocaleContext().getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+2"), ((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone());

		LocaleContextHolder.setTimeZone(null);
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
		assertNull(LocaleContextHolder.getLocaleContext());
	}

	@Test
	public void testSetLocaleAndSetTimeZoneMixed() {
		LocaleContextHolder.setLocale(Locale.GERMANY);
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
		assertFalse(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocaleContext().getLocale());

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+1"));
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+1"), LocaleContextHolder.getTimeZone());
		assertTrue(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertEquals(Locale.GERMANY, LocaleContextHolder.getLocaleContext().getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+1"), ((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone());

		LocaleContextHolder.setLocale(Locale.GERMAN);
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+1"), LocaleContextHolder.getTimeZone());
		assertTrue(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocaleContext().getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+1"), ((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone());

		LocaleContextHolder.setTimeZone(null);
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
		assertFalse(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocaleContext().getLocale());

		LocaleContextHolder.setTimeZone(TimeZone.getTimeZone("GMT+2"));
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+2"), LocaleContextHolder.getTimeZone());
		assertTrue(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertEquals(Locale.GERMAN, LocaleContextHolder.getLocaleContext().getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+2"), ((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone());

		LocaleContextHolder.setLocale(null);
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+2"), LocaleContextHolder.getTimeZone());
		assertTrue(LocaleContextHolder.getLocaleContext() instanceof TimeZoneAwareLocaleContext);
		assertNull(LocaleContextHolder.getLocaleContext().getLocale());
		assertEquals(TimeZone.getTimeZone("GMT+2"), ((TimeZoneAwareLocaleContext) LocaleContextHolder.getLocaleContext()).getTimeZone());

		LocaleContextHolder.setTimeZone(null);
		assertEquals(Locale.getDefault(), LocaleContextHolder.getLocale());
		assertEquals(TimeZone.getDefault(), LocaleContextHolder.getTimeZone());
		assertNull(LocaleContextHolder.getLocaleContext());
	}

}
