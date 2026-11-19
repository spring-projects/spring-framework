/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
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

	@Test
	fun testStaticQueryWithNullableStringValueClass() {
		val mock = Mock()
		given(mock.resultSet.getString(1)).willReturn(null)
		val result = mock.jdbcTemplate.query(
			"select name, age from people",
			DataClassRowMapper(PersonWithNullableName::class.java)
		)
		assertThat(result).containsExactly(PersonWithNullableName(null, 22L))
	}

	@Test
	fun testStaticQueryWithNullablePrimitiveValueClass() {
		val mock = Mock()
		given(mock.resultSet.getObject(2)).willReturn(null)
		val result = mock.jdbcTemplate.query(
			"select name, age from people",
			DataClassRowMapper(PersonWithNullableAge::class.java)
		)
		assertThat(result).containsExactly(PersonWithNullableAge("Bubba", null))
	}


	data class KotlinPerson(var name: String, val age: Long, val birth_date: Date, val balance: BigDecimal) {
		init {
			name += " appended by init"
		}
	}

	@JvmInline
	value class Name(val value: String)

	@JvmInline
	value class Age(val value: Int)

	data class PersonWithNullableName(val name: Name?, val age: Long)

	data class PersonWithNullableAge(val name: String, val age: Age?)

}
