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

package org.springframework.beans.propertyeditors;

import java.util.ResourceBundle;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link ResourceBundleEditor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class ResourceBundleEditorTests {

	private static final String BASE_NAME = ResourceBundleEditorTests.class.getName();

	private static final String MESSAGE_KEY = "punk";


	@Test
	public void testSetAsTextWithJustBaseName() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME);
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		boolean condition = value instanceof ResourceBundle;
		assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo(MESSAGE_KEY);
	}

	@Test
	public void testSetAsTextWithBaseNameThatEndsInDefaultSeparator() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "_");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		boolean condition = value instanceof ResourceBundle;
		assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo(MESSAGE_KEY);
	}

	@Test
	public void testSetAsTextWithBaseNameAndLanguageCode() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "Lang" + "_en");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		boolean condition = value instanceof ResourceBundle;
		assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo("yob");
	}

	@Test
	public void testSetAsTextWithBaseNameLanguageAndCountryCode() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "LangCountry" + "_en_GB");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		boolean condition = value instanceof ResourceBundle;
		assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo("chav");
	}

	@Test
	public void testSetAsTextWithTheKitchenSink() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		editor.setAsText(BASE_NAME + "LangCountryDialect" + "_en_GB_GLASGOW");
		Object value = editor.getValue();
		assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
		boolean condition = value instanceof ResourceBundle;
		assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
		ResourceBundle bundle = (ResourceBundle) value;
		String string = bundle.getString(MESSAGE_KEY);
		assertThat(string).isEqualTo("ned");
	}

	@Test
	public void testSetAsTextWithNull() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(null));
	}

	@Test
	public void testSetAsTextWithEmptyString() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText(""));
	}

	@Test
	public void testSetAsTextWithWhiteSpaceString() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText("   "));
	}

	@Test
	public void testSetAsTextWithJustSeparatorString() throws Exception {
		ResourceBundleEditor editor = new ResourceBundleEditor();
		assertThatIllegalArgumentException().isThrownBy(() ->
				editor.setAsText("_"));
	}

}
