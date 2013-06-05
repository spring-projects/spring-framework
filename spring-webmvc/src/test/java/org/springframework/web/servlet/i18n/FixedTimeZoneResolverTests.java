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

import java.util.TimeZone;

import junit.framework.TestCase;

/**
 * @author Nicholas Williams
 */
public class FixedTimeZoneResolverTests extends TestCase {

	private static final TimeZone SYSTEM = TimeZone.getDefault();

	private static final String SYSTEM_ID = SYSTEM.getID();

	public void testResolveSystemDefaultTimeZone() {
		FixedTimeZoneResolver resolver = new FixedTimeZoneResolver();
		TimeZone timeZone = resolver.resolveTimeZone(null);

		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", SYSTEM_ID, timeZone.getID());
	}

	public void testResolveAmericaChicagoTimeZone() {
		FixedTimeZoneResolver resolver = new FixedTimeZoneResolver(TimeZone.getTimeZone("America/Chicago"));
		TimeZone timeZone = resolver.resolveTimeZone(null);

		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testResolveAmericaLosAngelesTimeZone() {
		FixedTimeZoneResolver resolver = new FixedTimeZoneResolver(TimeZone.getTimeZone("America/Los_Angeles"));
		TimeZone timeZone = resolver.resolveTimeZone(null);

		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testSetDefaultResolveAmericaChicagoTimeZone() {
		FixedTimeZoneResolver resolver = new FixedTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("America/Chicago"));
		TimeZone timeZone = resolver.resolveTimeZone(null);

		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Chicago", timeZone.getID());
	}

	public void testSetDefaultResolveAmericaLosAngelesTimeZone() {
		FixedTimeZoneResolver resolver = new FixedTimeZoneResolver();
		resolver.setDefaultTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
		TimeZone timeZone = resolver.resolveTimeZone(null);

		assertNotNull("The time zone should not be null.", timeZone);
		assertEquals("The time zone is not correct.", "America/Los_Angeles", timeZone.getID());
	}

	public void testCantSetTimeZone() {
		FixedTimeZoneResolver resolver = new FixedTimeZoneResolver();
		try {
			resolver.setTimeZone(null, null, null);
			fail("You should not be able to set the time zone on the fixed resolver.");
		} catch (UnsupportedOperationException e) {
			// good
		}
	}

}
