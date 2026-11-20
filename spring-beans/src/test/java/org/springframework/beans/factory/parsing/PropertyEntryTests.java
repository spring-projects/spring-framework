/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link PropertyEntry}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class PropertyEntryTests {

	@Test
	void ctorBailsOnNullPropertyNameArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertyEntry(null));
	}

	@Test
	void ctorBailsOnEmptyPropertyNameArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertyEntry(""));
	}

	@Test
	void ctorBailsOnWhitespacedPropertyNameArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new PropertyEntry("\t   "));
	}

}
