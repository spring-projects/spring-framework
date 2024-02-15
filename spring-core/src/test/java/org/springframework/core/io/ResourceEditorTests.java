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

package org.springframework.core.io;

import java.beans.PropertyEditor;

import org.junit.jupiter.api.Test;

import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.PlaceholderResolutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ResourceEditor}.
 *
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Dave Syer
 */
class ResourceEditorTests {

	@Test
	void sunnyDay() {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText("classpath:org/springframework/core/io/ResourceEditorTests.class");
		Resource resource = (Resource) editor.getValue();
		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();
	}

	@Test
	void ctorWithNullCtorArgs() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ResourceEditor(null, null));
	}

	@Test
	void setAndGetAsTextWithNull() {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText(null);
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void setAndGetAsTextWithWhitespaceResource() {
		PropertyEditor editor = new ResourceEditor();
		editor.setAsText("  ");
		assertThat(editor.getAsText()).isEmpty();
	}

	@Test
	void systemPropertyReplacement() {
		PropertyEditor editor = new ResourceEditor();
		System.setProperty("test.prop", "foo");
		try {
			editor.setAsText("${test.prop}");
			Resource resolved = (Resource) editor.getValue();
			assertThat(resolved.getFilename()).isEqualTo("foo");
		}
		finally {
			System.clearProperty("test.prop");
		}
	}

	@Test
	void systemPropertyReplacementWithUnresolvablePlaceholder() {
		PropertyEditor editor = new ResourceEditor();
		System.setProperty("test.prop", "foo");
		try {
			editor.setAsText("${test.prop}-${bar}");
			Resource resolved = (Resource) editor.getValue();
			assertThat(resolved.getFilename()).isEqualTo("foo-${bar}");
		}
		finally {
			System.clearProperty("test.prop");
		}
	}

	@Test
	void strictSystemPropertyReplacementWithUnresolvablePlaceholder() {
		PropertyEditor editor = new ResourceEditor(new DefaultResourceLoader(), new StandardEnvironment(), false);
		System.setProperty("test.prop", "foo");
		try {
			assertThatExceptionOfType(PlaceholderResolutionException.class).isThrownBy(() -> {
					editor.setAsText("${test.prop}-${bar}");
					editor.getValue();
			});
		}
		finally {
			System.clearProperty("test.prop");
		}
	}

}
