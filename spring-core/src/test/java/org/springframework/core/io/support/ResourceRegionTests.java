/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.io.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ResourceRegion}.
 *
 * @author Brian Clozel
 */
class ResourceRegionTests {

	@Test
	void shouldThrowExceptionWithNullResource() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourceRegion(null, 0, 1));
	}

	@Test
	void shouldThrowExceptionForNegativePosition() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourceRegion(mock(), -1, 1));
	}

	@Test
	void shouldThrowExceptionForNegativeCount() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new ResourceRegion(mock(), 0, -1));
	}

}
