/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
