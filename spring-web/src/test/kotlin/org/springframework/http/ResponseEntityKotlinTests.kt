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
	fun ofNullNullableType() {
		val responseEntity = ResponseEntity.ofNullable<Int?>(null)
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
		assertThat(responseEntity.body).isNull()
	}

	@Test
	fun okNullNullableType() {
		val responseEntity = ResponseEntity.ok<String?>(null)
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(responseEntity.body).isNull()
	}

	@Test
	fun okUnitType() {
		val responseEntity: ResponseEntity<Unit> = ResponseEntity.ok().build()
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(responseEntity.body).isEqualTo(Unit)
	}

	@Test
	fun okNullNullableUnitType() {
		val responseEntity = ResponseEntity.ok<Unit?>(null)
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.OK)
		assertThat(responseEntity.body).isNull()
	}

	@Test
	fun noContentUnitType() {
		val responseEntity = ResponseEntity.noContent().build<Unit>()
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
		assertThat(responseEntity.body).isEqualTo(Unit)
	}

	@Test
	fun noContentNullableUnitType() {
		val responseEntity: ResponseEntity<Unit?> = ResponseEntity.noContent().build()
		assertThat(responseEntity).isNotNull()
		assertThat(responseEntity.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
		assertThat(responseEntity.body).isNull()
	}
}
