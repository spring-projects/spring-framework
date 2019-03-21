/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.mail.javamail;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Brian Hanafee
 * @author Sam Brannen
 * @since 09.07.2005
 */
public class InternetAddressEditorTests {

	private static final String EMPTY = "";
	private static final String SIMPLE = "nobody@nowhere.com";
	private static final String BAD = "(";

	private final InternetAddressEditor editor = new InternetAddressEditor();


	@Test
	public void uninitialized() {
		assertEquals("Uninitialized editor did not return empty value string", EMPTY, editor.getAsText());
	}

	@Test
	public void setNull() {
		editor.setAsText(null);
		assertEquals("Setting null did not result in empty value string", EMPTY, editor.getAsText());
	}

	@Test
	public void setEmpty() {
		editor.setAsText(EMPTY);
		assertEquals("Setting empty string did not result in empty value string", EMPTY, editor.getAsText());
	}

	@Test
	public void allWhitespace() {
		editor.setAsText(" ");
		assertEquals("All whitespace was not recognized", EMPTY, editor.getAsText());
	}

	@Test
	public void simpleGoodAddress() {
		editor.setAsText(SIMPLE);
		assertEquals("Simple email address failed", SIMPLE, editor.getAsText());
	}

	@Test
	public void excessWhitespace() {
		editor.setAsText(" " + SIMPLE + " ");
		assertEquals("Whitespace was not stripped", SIMPLE, editor.getAsText());
	}

	@Test(expected = IllegalArgumentException.class)
	public void simpleBadAddress() {
		editor.setAsText(BAD);
	}

}
