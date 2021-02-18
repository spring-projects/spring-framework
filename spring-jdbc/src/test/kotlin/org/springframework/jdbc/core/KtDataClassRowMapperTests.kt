package org.springframework.jdbc.core

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.test.ConstructorPerson
import java.math.BigDecimal
import java.util.*

class KtDataClassRowMapperTests : AbstractRowMapperTests() {

	@Test
	fun testStaticQueryWithDataClass() {
		val mock = Mock()
		val result = mock.jdbcTemplate.query(
			"select name, age, birth_date, balance from people",
			DataClassRowMapper(ConstructorPerson::class.java)
		)
		Assertions.assertThat(result.size).isEqualTo(1)
		verifyPerson(result[0])
		mock.verifyClosed()
	}

	data class KotlinPerson(var name: String, val age: Long, val birth_date: Date, val balance: BigDecimal) {
		init {
			name += " appended by init"
		}
	}

	@Test
	fun testInitPropertiesAreNotOverriden() {
		val mock = Mock()
		val result = mock.jdbcTemplate.query(
			"select name, age, birth_date, balance from people",
			DataClassRowMapper(KotlinPerson::class.java)
		)
		Assertions.assertThat(result.size).isEqualTo(1)
		Assertions.assertThat(result[0].name).isEqualTo("Bubba appended by init")
	}
}

