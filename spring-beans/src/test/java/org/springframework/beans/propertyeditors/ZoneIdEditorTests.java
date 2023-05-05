/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nicholas Williams
 * @author Sam Brannen
 */
class ZoneIdEditorTests {

	private final ZoneIdEditor editor = new ZoneIdEditor();

	@ParameterizedTest(name = "[{index}] text = ''{0}''")
	@ValueSource(strings = {
		"America/Chicago",
		"   America/Chicago   ",
	})
	void americaChicago(String text) {
		editor.setAsText(text);

		ZoneId zoneId = (ZoneId) editor.getValue();
		assertThat(zoneId).as("The zone ID should not be null.").isNotNull();
		assertThat(zoneId).as("The zone ID is not correct.").isEqualTo(ZoneId.of("America/Chicago"));

		assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/Chicago");
	}

	@Test
	void americaLosAngeles() {
		editor.setAsText("America/Los_Angeles");

		ZoneId zoneId = (ZoneId) editor.getValue();
		assertThat(zoneId).as("The zone ID should not be null.").isNotNull();
		assertThat(zoneId).as("The zone ID is not correct.").isEqualTo(ZoneId.of("America/Los_Angeles"));

		assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/Los_Angeles");
	}

	@Test
	void getNullAsText() {
		assertThat(editor.getAsText()).as("The returned value is not correct.").isEmpty();
	}

	@Test
	void getValueAsText() {
		editor.setValue(ZoneId.of("America/New_York"));
		assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/New_York");
	}

}
