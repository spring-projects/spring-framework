/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.test.ConstructorPerson
import java.math.BigDecimal
import java.util.*

class DataClassRowMapperKotlinTests : AbstractRowMapperTests() {

	@Test
	fun testStaticQueryWithDataClass() {
		val mock = Mock()
		val result = mock.jdbcTemplate.query(
			"select name, age, birth_date, balance from people",
			DataClassRowMapper(ConstructorPerson::class.java)
		)
		assertThat(result.size).isEqualTo(1)
		verifyPerson(result[0])
		mock.verifyClosed()
	}

	@Test
	fun testInitPropertiesAreNotOverridden() {
		val mock = Mock()
		val result = mock.jdbcTemplate.query(
			"select name, age, birth_date, balance from people",
			DataClassRowMapper(KotlinPerson::class.java)
		)
		assertThat(result.size).isEqualTo(1)
		assertThat(result[0].name).isEqualTo("Bubba appended by init")
	}


	data class KotlinPerson(var name: String, val age: Long, val birth_date: Date, val balance: BigDecimal) {
		init {
			name += " appended by init"
		}
	}

}
