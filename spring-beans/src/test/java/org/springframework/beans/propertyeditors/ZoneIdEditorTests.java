/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Nicholas Williams
 * @author Sam Brannen
 * @author Juergen Hoeller
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

	@Test
	void correctExceptionForInvalid() {
		assertThatIllegalArgumentException().isThrownBy(() -> editor.setAsText("INVALID")).withMessageContaining("INVALID");
	}

}
