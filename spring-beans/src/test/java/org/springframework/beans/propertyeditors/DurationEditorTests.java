/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;

/**
 * @author Piotr Findeisen
 */
public class DurationEditorTests {

	@Test
	public void setSimpleFormat() {
		// Given
		DurationEditor editor = new DurationEditor();
		// When
		editor.setAsText("2 ms");
		// Then
		Duration duration = (Duration) editor.getValue();
		assertEquals("The set Duration is not correct.", Duration.ofMillis(2), duration);
	}

	@Test
	public void setSimpleFormatWithoutSpace() {
		// Given
		DurationEditor editor = new DurationEditor();
		// When
		editor.setAsText("2h");
		// Then
		Duration duration = (Duration) editor.getValue();
		assertEquals("The set Duration is not correct.", Duration.ofHours(2), duration);
	}

	@Test
	public void setSimpleFormatNegative() {
		// Given
		DurationEditor editor = new DurationEditor();
		// When
		editor.setAsText("-1d");
		// Then
		Duration duration = (Duration) editor.getValue();
		assertEquals("The set Duration is not correct.", Duration.ofDays(-1), duration);
	}

	@Test
	public void setISO8601Format() {
		// Given
		DurationEditor editor = new DurationEditor();
		// When
		editor.setAsText("PT19s");
		// Then
		Duration duration = (Duration) editor.getValue();
		assertEquals("The set Duration is not correct.", Duration.ofSeconds(19), duration);
	}

	@Test
	public void getValueAsText() {
		// Given
		DurationEditor editor = new DurationEditor();
		// When
		editor.setValue(Duration.ofSeconds(3));
		// Then
		assertEquals("The text version is not correct.", "PT3S", editor.getAsText());
	}

}
