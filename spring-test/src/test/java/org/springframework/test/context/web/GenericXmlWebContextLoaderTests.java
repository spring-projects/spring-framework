/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GenericXmlWebContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0.4
 */
class GenericXmlWebContextLoaderTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];


	@Test
	void configMustNotContainAnnotatedClasses() {
		GenericXmlWebContextLoader loader = new GenericXmlWebContextLoader();
		@SuppressWarnings("deprecation")
		WebMergedContextConfiguration mergedConfig = new WebMergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
				new Class<?>[] { getClass() }, null, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY, EMPTY_STRING_ARRAY,
				"resource/path", loader, null, null);
		assertThatIllegalStateException()
				.isThrownBy(() -> loader.loadContext(mergedConfig))
				.withMessageContaining("does not support annotated classes");
	}

}
