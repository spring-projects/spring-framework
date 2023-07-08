/*
 * Copyright 2002-2023 the original author or authors
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
