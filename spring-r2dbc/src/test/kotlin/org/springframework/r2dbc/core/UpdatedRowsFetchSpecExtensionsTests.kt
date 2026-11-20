/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono

/**
 * Tests for [UpdatedRowsFetchSpec] extensions.
 *
 * @author Fred Montariol
 */
class UpdatedRowsFetchSpecExtensionsTests {

	@Test
	suspend fun awaitRowsUpdatedWithValue() {
		val spec = mockk<UpdatedRowsFetchSpec>()
		every { spec.rowsUpdated() } returns Mono.just(42)
		assertThat(spec.awaitRowsUpdated()).isEqualTo(42)
		verify {
			spec.rowsUpdated()
		}
	}

}
