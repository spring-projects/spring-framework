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

import java.util.TimeZone;

import junit.framework.TestCase;

/**
 * @author Nicholas Williams
 */
public class SimpleTimeZoneContextTests extends TestCase {

	public void testNullConstructorArgumentFails() {
		try {
			new SimpleTimeZoneContext(null);
			fail("Expected IllegalArgumentException, got no exception.");
		}
		catch (IllegalArgumentException e) {
			// good
		}
	}

	public void testTimeZonePropertyAmericaChicago() {
		TimeZone timeZone = TimeZone.getTimeZone("America/Chicago");

		SimpleTimeZoneContext context = new SimpleTimeZoneContext(timeZone);

		assertNotNull("The time zone should not be null.", context.getTimeZone());
		assertEquals("The time zone is not correct.", "America/Chicago", context.getTimeZone().getID());
		assertEquals("The string is not correct.", "America/Chicago", context.toString());
	}

	public void testTimeZonePropertyAmericaLosAngeles() {
		TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");

		SimpleTimeZoneContext context = new SimpleTimeZoneContext(timeZone);

		assertNotNull("The time zone should not be null.", context.getTimeZone());
		assertEquals("The time zone is not correct.", "America/Los_Angeles", context.getTimeZone().getID());
		assertEquals("The string is not correct.", "America/Los_Angeles", context.toString());
	}

}
