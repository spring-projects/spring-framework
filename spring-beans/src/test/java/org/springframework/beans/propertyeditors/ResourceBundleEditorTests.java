/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ResourceBundleEditor}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class ResourceBundleEditorTests {

	private static final String BASE_NAME = ResourceBundleEditorTests.class.getName();

	private static final String MESSAGE_KEY = "punk";


	@Test
	void testSetAsTextWithJustBaseName() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME);
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		assertThat(value instanceof ResourceBundle).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo(MESSAGE_KEY);
	}

	@Test
	void testSetAsTextWithBaseNameThatEndsInDefaultSeparator() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "_");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		assertThat(value instanceof ResourceBundle).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo(MESSAGE_KEY);
	}

	@Test
	void testSetAsTextWithBaseNameAndLanguageCode() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "Lang" + "_en");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		assertThat(value instanceof ResourceBundle).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo("yob");
	}

	@Test
	void testSetAsTextWithBaseNameLanguageAndCountryCode() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "LangCountry" + "_en_GB");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		assertThat(value instanceof ResourceBundle).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo("chav");
	}

	@Test
	void testSetAsTextWithTheKitchenSink() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "LangCountryDialect" + "_en_GB_GLASGOW");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		assertThat(value instanceof ResourceBundle).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo("ned");
	}

	@Test
	void testSetAsTextWithNull() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(null));
	}

	@Test
	void testSetAsTextWithEmptyString() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(""));
	}

	@Test
	void testSetAsTextWithWhiteSpaceString() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText("   "));
	}

	@Test
	void testSetAsTextWithJustSeparatorString() {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText("_"));
	}

}
