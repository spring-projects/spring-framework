/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Kotlin tests for [ResponseEntity].
 *
 * @author Sebastien Deleuze
 */
class ResponseEntityKotlinTests {

	@Test
	fun ofNullable() {
		val entity = 42
		val responseEntity = ResponseEntity.ofNullable(entity)
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(responseEntity.body).isEqualTo(entity)
	}

	@Test
	fun ofNullNullable() {
		val responseEntity = ResponseEntity.ofNullable<Int>(null)
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
		assertThat(responseEntity.body).isNull()
	}

}
