/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link AnnotationConfigWebContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0.4
 */
class AnnotationConfigWebContextLoaderTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];
	private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];


	@Test
	@SuppressWarnings("deprecation")
	void configMustNotContainLocations() {
		AnnotationConfigWebContextLoader loader = new AnnotationConfigWebContextLoader();
		WebMergedContextConfiguration mergedConfig = new WebMergedContextConfiguration(getClass(),
				new String[] { "config.xml" }, EMPTY_CLASS_ARRAY, null, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY,
				EMPTY_STRING_ARRAY, "resource/path", loader, null, null);
		assertThatIllegalStateException()
				.isThrownBy(() -> loader.loadContext(mergedConfig))
				.withMessageContaining("does not support resource locations");
	}

}
