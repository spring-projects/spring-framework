/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans;

import java.beans.PropertyEditor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import org.springframework.beans.propertyeditors.CustomNumberEditor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PropertyEditorRegistrySupport}.
 *
 * @author Sam Brannen
 * @since 7.1
 */
class PropertyEditorRegistrySupportTests {

	/**
	 * Matches the private {@code MAX_STRIPPED_PROPERTY_PATH_DEPTH} constant in
	 * {@link PropertyEditorRegistrySupport}.
	 */
	private static final int MAX_DEPTH = 8;

	private final PropertyEditorRegistrySupport registry = new PropertyEditorRegistrySupport();

	private final PropertyEditor editor = new CustomNumberEditor(Integer.class, true);


	@Test
	void findCustomEditorMatchesStrippedPathAtMaxSupportedNestingDepth() {
		registry.registerCustomEditor(null, "list", this.editor);

		String propertyPath = "list" + "[0]".repeat(MAX_DEPTH);
		assertThat(registry.findCustomEditor(null, propertyPath)).isSameAs(this.editor);
	}

	@Test
	void findCustomEditorDoesNotMatchStrippedPathBeyondMaxSupportedNestingDepth() {
		registry.registerCustomEditor(null, "list", this.editor);

		String propertyPath = "list" + "[0]".repeat(MAX_DEPTH + 1);
		assertThat(registry.findCustomEditor(null, propertyPath)).isNull();
	}

	@Test  // gh-37020
	@Timeout(5)
	void findCustomEditorWithExcessivelyNestedPropertyPathDoesNotHang() {
		registry.registerCustomEditor(null, "attrs", this.editor);

		// A property path with a bracket-nesting depth (40) that would previously
		// have caused addStrippedPropertyPaths() to recursively enumerate 2^40 - 1
		// stripped path variants. With the depth limit in place, this should return
		// promptly.
		String propertyPath = "attrs" + "[k]".repeat(40);

		assertThat(registry.findCustomEditor(null, propertyPath)).isNull();
	}

	@Test
	void guessPropertyTypeFromEditorsMatchesStrippedPathAtMaxSupportedNestingDepth() {
		registry.registerCustomEditor(Integer.class, "list", this.editor);

		String propertyPath = "list" + "[0]".repeat(MAX_DEPTH);
		assertThat(registry.guessPropertyTypeFromEditors(propertyPath)).isEqualTo(Integer.class);
	}

	@Test
	void guessPropertyTypeFromEditorsDoesNotMatchStrippedPathBeyondMaxSupportedNestingDepth() {
		registry.registerCustomEditor(Integer.class, "list", this.editor);

		String propertyPath = "list" + "[0]".repeat(MAX_DEPTH + 1);
		assertThat(registry.guessPropertyTypeFromEditors(propertyPath)).isNull();
	}

}
