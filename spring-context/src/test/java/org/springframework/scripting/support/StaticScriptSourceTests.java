/*
 * Copyright 2002-2015 the original author or authors.
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

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for the StaticScriptSource class.
 *
 * @author Rick Evans
 * @author Sam Brannen
 */
public final class StaticScriptSourceTests {

	private static final String SCRIPT_TEXT = "print($hello) if $true;";

	private final StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);


	@Test(expected = IllegalArgumentException.class)
	public void createWithNullScript() throws Exception {
		new StaticScriptSource(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithEmptyScript() throws Exception {
		new StaticScriptSource("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void createWithWhitespaceOnlyScript() throws Exception {
		new StaticScriptSource("   \n\n\t  \t\n");
	}

	@Test
	public void isModifiedIsTrueByDefault() throws Exception {
		assertTrue("Script must be flagged as 'modified' when first created.", source.isModified());
	}

	@Test
	public void gettingScriptTogglesIsModified() throws Exception {
		source.getScriptAsString();
		assertFalse("Script must be flagged as 'not modified' after script is read.", source.isModified());
	}

	@Test
	public void gettingScriptViaToStringDoesNotToggleIsModified() throws Exception {
		boolean isModifiedState = source.isModified();
		source.toString();
		assertEquals("Script's 'modified' flag must not change after script is read via toString().", isModifiedState, source.isModified());
	}

	@Test
	public void isModifiedToggledWhenDifferentScriptIsSet() throws Exception {
		source.setScript("use warnings;");
		assertTrue("Script must be flagged as 'modified' when different script is passed in.", source.isModified());
	}

	@Test
	public void isModifiedNotToggledWhenSameScriptIsSet() throws Exception {
		source.setScript(SCRIPT_TEXT);
		assertFalse("Script must not be flagged as 'modified' when same script is passed in.", source.isModified());
	}

}
