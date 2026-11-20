/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.parsing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConstructorArgumentEntry}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class ConstructorArgumentEntryTests {

	@Test
	void ctorBailsOnNegativeCtorIndexArgument() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new ConstructorArgumentEntry(-1));
	}

}
