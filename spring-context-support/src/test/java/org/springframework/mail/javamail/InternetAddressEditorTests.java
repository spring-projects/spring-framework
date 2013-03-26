/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.mail.javamail;

import junit.framework.TestCase;

/**
 * @author Brian Hanafee
 * @since 09.07.2005
 */
public class InternetAddressEditorTests extends TestCase {

	private static final String EMPTY = "";
	private static final String SIMPLE = "nobody@nowhere.com";
	private static final String BAD = "(";

	private InternetAddressEditor editor;

	@Override
	protected void setUp() {
		editor = new InternetAddressEditor();
	}

	public void testUninitialized() {
		assertEquals("Uninitialized editor did not return empty value string", EMPTY, editor.getAsText());
	}

	public void testSetNull() {
		editor.setAsText(null);
		assertEquals("Setting null did not result in empty value string", EMPTY, editor.getAsText());
	}

	public void testSetEmpty() {
		editor.setAsText(EMPTY);
		assertEquals("Setting empty string did not result in empty value string", EMPTY, editor.getAsText());
	}

	public void testAllWhitespace() {
		editor.setAsText(" ");
		assertEquals("All whitespace was not recognized", EMPTY, editor.getAsText());
	}

	public void testSimpleGoodAddess() {
		editor.setAsText(SIMPLE);
		assertEquals("Simple email address failed", SIMPLE, editor.getAsText());
	}

	public void testExcessWhitespace() {
		editor.setAsText(" " + SIMPLE + " ");
		assertEquals("Whitespace was not stripped", SIMPLE, editor.getAsText());
	}

	public void testSimpleBadAddress() {
		try {
			editor.setAsText(BAD);
			fail("Should have failed on \"" + BAD + "\", instead got " + editor.getAsText());
		}
		catch (IllegalArgumentException e) {
			// Passed the test
		}
	}

}
