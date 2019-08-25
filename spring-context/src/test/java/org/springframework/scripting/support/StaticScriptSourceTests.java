/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.scripting.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the StaticScriptSource class.
 *
 * @author Rick Evans
 * @author Sam Brannen
 */
public class StaticScriptSourceTests {

	private static final String SCRIPT_TEXT = "print($hello) if $true;";

	private final StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);


	@Test
	public void createWithNullScript() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StaticScriptSource(null));
	}

	@Test
	public void createWithEmptyScript() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StaticScriptSource(""));
	}

	@Test
	public void createWithWhitespaceOnlyScript() throws Exception {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new StaticScriptSource("   \n\n\t  \t\n"));
	}

	@Test
	public void isModifiedIsTrueByDefault() throws Exception {
		assertThat(source.isModified()).as("Script must be flagged as 'modified' when first created.").isTrue();
	}

	@Test
	public void gettingScriptTogglesIsModified() throws Exception {
		source.getScriptAsString();
		assertThat(source.isModified()).as("Script must be flagged as 'not modified' after script is read.").isFalse();
	}

	@Test
	public void gettingScriptViaToStringDoesNotToggleIsModified() throws Exception {
		boolean isModifiedState = source.isModified();
		source.toString();
		assertThat(source.isModified()).as("Script's 'modified' flag must not change after script is read via toString().").isEqualTo(isModifiedState);
	}

	@Test
	public void isModifiedToggledWhenDifferentScriptIsSet() throws Exception {
		source.setScript("use warnings;");
		assertThat(source.isModified()).as("Script must be flagged as 'modified' when different script is passed in.").isTrue();
	}

	@Test
	public void isModifiedNotToggledWhenSameScriptIsSet() throws Exception {
		source.setScript(SCRIPT_TEXT);
		assertThat(source.isModified()).as("Script must not be flagged as 'modified' when same script is passed in.").isFalse();
	}

}
