/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core.namedparam

import org.assertj.core.api.Assertions.assertThat
import java.sql.JDBCType

import org.junit.jupiter.api.Test

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
		assertThat(source.getValue("foo")).isEqualTo(2)
	}

	@Test
	fun `setter with value and type`() {
		val source = MapSqlParameterSource()
		source["foo", JDBCType.INTEGER.vendorTypeNumber] = 2
		assertThat(source.getValue("foo")).isEqualTo(2)
		assertThat(source.getSqlType("foo")).isEqualTo(JDBCType.INTEGER.vendorTypeNumber)
	}

	@Test
	fun `setter with value, type and type name`() {
		val source = MapSqlParameterSource()
		source["foo", JDBCType.INTEGER.vendorTypeNumber, "INT"] = 2
		assertThat(source.getValue("foo")).isEqualTo(2)
		assertThat(source.getSqlType("foo")).isEqualTo(JDBCType.INTEGER.vendorTypeNumber)
		assertThat(source.getTypeName("foo")).isEqualTo("INT")
	}

}
