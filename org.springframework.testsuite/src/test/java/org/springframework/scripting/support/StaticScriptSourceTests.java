/*
 * Copyright 2002-2006 the original author or authors.
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

package org.springframework.scripting.support;

import junit.framework.TestCase;

/**
 * Unit tests for the StaticScriptSource class.
 *
 * @author Rick Evans
 */
public final class StaticScriptSourceTests extends TestCase {

	private static final String SCRIPT_TEXT = "print($hello) if $true;";


	public void testCreateWithNullScript() throws Exception {
		try {
			new StaticScriptSource(null);
			fail("Must have failed when passed a null script string.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCreateWithEmptyScript() throws Exception {
		try {
			new StaticScriptSource("");
			fail("Must have failed when passed an empty script string.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCreateWithWhitespaceOnlyScript() throws Exception {
		try {
			new StaticScriptSource("   \n\n\t  \t\n");
			fail("Must have failed when passed a whitespace-only script string.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testIsModifiedIsTrueByDefault() throws Exception {
		StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);
		assertTrue("Script must be flagged as 'modified' when first created.", source.isModified());
	}

	public void testGettingScriptTogglesIsModified() throws Exception {
		StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);
		source.getScriptAsString();
		assertFalse("Script must be flagged as 'not modified' after script is read.", source.isModified());
	}

	public void testGettingScriptViaToStringDoesNotToggleIsModified() throws Exception {
		StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);
		boolean isModifiedState = source.isModified();
		source.toString();
		assertEquals("Script's 'modified' flag must not change after script is read via toString().", isModifiedState, source.isModified());
	}

	public void testIsModifiedToggledWhenDifferentScriptIsSet() throws Exception {
		StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);
		source.setScript("use warnings;");
		assertTrue("Script must be flagged as 'modified' when different script is passed in.", source.isModified());
	}

	public void testIsModifiedNotToggledWhenSameScriptIsSet() throws Exception {
		StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);
		source.setScript(SCRIPT_TEXT);
		assertFalse("Script must not be flagged as 'modified' when same script is passed in.", source.isModified());
	}

}
