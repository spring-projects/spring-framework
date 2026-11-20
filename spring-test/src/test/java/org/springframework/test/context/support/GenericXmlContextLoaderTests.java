/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.support;

import org.junit.jupiter.api.Test;

import org.springframework.test.context.MergedContextConfiguration;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link GenericXmlContextLoader}.
 *
 * @author Sam Brannen
 * @since 4.0.4
 * @see GenericXmlContextLoaderResourceLocationsTests
 */
class GenericXmlContextLoaderTests {

	private static final String[] EMPTY_STRING_ARRAY = new String[0];


	@Test
	void configMustNotContainAnnotatedClasses() {
		GenericXmlContextLoader loader = new GenericXmlContextLoader();
		MergedContextConfiguration mergedConfig = new MergedContextConfiguration(getClass(), EMPTY_STRING_ARRAY,
			new Class<?>[] { getClass() }, EMPTY_STRING_ARRAY, loader);
		assertThatIllegalStateException()
			.isThrownBy(() -> loader.loadContext(mergedConfig))
			.withMessageContaining("does not support annotated classes");
	}

}
