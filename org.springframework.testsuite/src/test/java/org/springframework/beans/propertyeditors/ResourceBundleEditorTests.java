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

package org.springframework.beans.propertyeditors;

import junit.framework.TestCase;
import org.springframework.test.AssertThrows;

import java.util.ResourceBundle;

/**
 * Unit tests for the {@link ResourceBundleEditor} class.
 *
 * @author Rick Evans
 */
public final class ResourceBundleEditorTests extends TestCase {

	private static final String BASE_NAME = ResourceBundleEditorTests.class.getName();
	private static final String MESSAGE_KEY = "punk";


	public void testSetAsTextWithJustBaseName() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME);
		Object value = editor.getValue();
		assertNotNull("Returned ResourceBundle was null (must not be for valid setAsText(..) call).", value);
		assertTrue("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).", value instanceof ResourceBundle);
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertEquals(MESSAGE_KEY, string);
	}

	public void testSetAsTextWithBaseNameThatEndsInDefaultSeparator() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "_");
		Object value = editor.getValue();
		assertNotNull("Returned ResourceBundle was null (must not be for valid setAsText(..) call).", value);
		assertTrue("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).", value instanceof ResourceBundle);
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertEquals(MESSAGE_KEY, string);
	}

	public void testSetAsTextWithBaseNameAndLanguageCode() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "Lang" + "_en");
		Object value = editor.getValue();
		assertNotNull("Returned ResourceBundle was null (must not be for valid setAsText(..) call).", value);
		assertTrue("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).", value instanceof ResourceBundle);
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertEquals("yob", string);
	}

	public void testSetAsTextWithBaseNameLanguageAndCountryCode() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "LangCountry" + "_en_GB");
		Object value = editor.getValue();
		assertNotNull("Returned ResourceBundle was null (must not be for valid setAsText(..) call).", value);
		assertTrue("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).", value instanceof ResourceBundle);
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertEquals("chav", string);
	}

	public void testSetAsTextWithTheKitchenSink() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "LangCountryDialect" + "_en_GB_GLASGOW");
		Object value = editor.getValue();
		assertNotNull("Returned ResourceBundle was null (must not be for valid setAsText(..) call).", value);
		assertTrue("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).", value instanceof ResourceBundle);
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertEquals("ned", string);
	}

	public void testSetAsTextWithNull() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				ResourceBundleEditor editor = new ResourceBundleEditor();
				editor.setAsText(null);
			}
		}.runTest();
	}

	public void testSetAsTextWithEmptyString() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				ResourceBundleEditor editor = new ResourceBundleEditor();
				editor.setAsText("");
			}
		}.runTest();
	}

	public void testSetAsTextWithWhiteSpaceString() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				ResourceBundleEditor editor = new ResourceBundleEditor();
				editor.setAsText("   ");
			}
		}.runTest();
	}

	public void testSetAsTextWithJustSeparatorString() throws Exception {
		new AssertThrows(IllegalArgumentException.class) {
			public void test() throws Exception {
				ResourceBundleEditor editor = new ResourceBundleEditor();
				editor.setAsText("_");
			}
		}.runTest();
	}

}
