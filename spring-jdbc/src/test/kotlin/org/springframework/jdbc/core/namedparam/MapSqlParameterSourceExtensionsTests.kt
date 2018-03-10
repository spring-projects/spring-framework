/*
 * Copyright 2002-2017 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.core.namedparam

import org.junit.Assert.assertEquals
import org.junit.Test
import java.sql.JDBCType

/**
 * Tests for [MapSqlParameterSource] Kotlin extensions.
 *
 * @author Mario Arias
 */
class MapSqlParameterSourceExtensionsTests {

	@Test
	fun `setter with value`() {
		val source = MapSqlParameterSource()
		source["foo"] = 2
		assertEquals(2, source.getValue("foo"))
	}

	@Test
	fun `setter with value and type`() {
		val source = MapSqlParameterSource()
		source["foo", JDBCType.INTEGER.vendorTypeNumber] = 2
		assertEquals(2, source.getValue("foo"))
		assertEquals(JDBCType.INTEGER.vendorTypeNumber, source.getSqlType("foo"))
	}

	@Test
	fun `setter with value, type and type name`() {
		val source = MapSqlParameterSource()
		source["foo", JDBCType.INTEGER.vendorTypeNumber, "INT"] = 2
		assertEquals(2, source.getValue("foo"))
		assertEquals(JDBCType.INTEGER.vendorTypeNumber, source.getSqlType("foo"))
		assertEquals("INT", source.getTypeName("foo"))
	}
}