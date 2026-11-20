/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link StaticScriptSource}.
 *
 * @author Rick Evans
 * @author Sam Brannen
 */
class StaticScriptSourceTests {

	private static final String SCRIPT_TEXT = "print($hello) if $true;";

	private final StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);


	@Test
	void createWithNullScript() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StaticScriptSource(null));
	}

	@Test
	void createWithEmptyScript() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StaticScriptSource(""));
	}

	@Test
	void createWithWhitespaceOnlyScript() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StaticScriptSource("   \n\n\t  \t\n"));
	}

	@Test
	void isModifiedIsTrueByDefault() {
		assertThat(source.isModified()).as("Script must be flagged as 'modified' when first created.").isTrue();
	}

	@Test
	void gettingScriptTogglesIsModified() {
		source.getScriptAsString();
		assertThat(source.isModified()).as("Script must be flagged as 'not modified' after script is read.").isFalse();
	}

	@Test
	void gettingScriptViaToStringDoesNotToggleIsModified() {
		boolean isModifiedState = source.isModified();
		source.toString();
		assertThat(source.isModified()).as("Script's 'modified' flag must not change after script is read via toString().").isEqualTo(isModifiedState);
	}

	@Test
	void isModifiedToggledWhenDifferentScriptIsSet() {
		source.setScript("use warnings;");
		assertThat(source.isModified()).as("Script must be flagged as 'modified' when different script is passed in.").isTrue();
	}

	@Test
	void isModifiedNotToggledWhenSameScriptIsSet() {
		source.setScript(SCRIPT_TEXT);
		assertThat(source.isModified()).as("Script must not be flagged as 'modified' when same script is passed in.").isFalse();
	}

}
