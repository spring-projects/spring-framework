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

package org.springframework.beans.propertyeditors;

import java.time.ZoneId;

import junit.framework.TestCase;

/**
 * @author Nicholas Williams
 */
public class ZoneIdEditorTests extends TestCase {

	public void testAmericaChicago() {
		ZoneIdEditor editor = new ZoneIdEditor();
		editor.setAsText("America/Chicago");

		ZoneId zoneId = (ZoneId) editor.getValue();
		assertNotNull("The zone ID should not be null.", zoneId);
		assertEquals("The zone ID is not correct.", ZoneId.of("America/Chicago"), zoneId);

		assertEquals("The text version is not correct.", "America/Chicago", editor.getAsText());
	}

	public void testAmericaLosAngeles() {
		ZoneIdEditor editor = new ZoneIdEditor();
		editor.setAsText("America/Los_Angeles");

		ZoneId zoneId = (ZoneId) editor.getValue();
		assertNotNull("The zone ID should not be null.", zoneId);
		assertEquals("The zone ID is not correct.", ZoneId.of("America/Los_Angeles"), zoneId);

		assertEquals("The text version is not correct.", "America/Los_Angeles", editor.getAsText());
	}

	public void testGetNullAsText() {
		ZoneIdEditor editor = new ZoneIdEditor();

		assertEquals("The returned value is not correct.", "", editor.getAsText());
	}

	public void testGetValueAsText() {
		ZoneIdEditor editor = new ZoneIdEditor();
		editor.setValue(ZoneId.of("America/New_York"));

		assertEquals("The text version is not correct.", "America/New_York", editor.getAsText());
	}

}
