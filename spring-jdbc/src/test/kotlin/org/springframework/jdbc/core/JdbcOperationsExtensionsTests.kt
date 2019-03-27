/*
 * Copyright 2002-2019 the original author or authors
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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.sql.*

/**
 * Mock object based tests for [JdbcOperations] Kotlin extensions
 *
 * @author Mario Arias
 * @author Sebastien Deleuze
 */
class JdbcOperationsExtensionsTests {

	val template = mockk<JdbcTemplate>()

	val sql = "select age from customer where id = 3"

	@Test
	fun `queryForObject with reified type parameters`() {
		every { template.queryForObject(sql, any<Class<Int>>()) } returns 2
		assertEquals(2, template.queryForObject<Int>(sql))
		verify { template.queryForObject(sql, any<Class<Int>>()) }
	}

	@Test
	fun `queryForObject with RowMapper-like function`() {
		every { template.queryForObject(sql, any<RowMapper<Int>>(), any<Int>()) } returns 2
		assertEquals(2, template.queryForObject(sql, 3) { rs: ResultSet, _: Int -> rs.getInt(1) })
		verify { template.queryForObject(eq(sql), any<RowMapper<Int>>(), eq(3)) }
	}

	@Test  // gh-22682
	fun `queryForObject with nullable RowMapper-like function`() {
		every { template.queryForObject(sql, any<RowMapper<Int>>(), 3) } returns null
		assertNull(template.queryForObject(sql, 3) { _, _ -> null as Int? })
		verify { template.queryForObject(eq(sql), any<RowMapper<Int?>>(), eq(3)) }
	}

	@Test
	fun `queryForObject with reified type parameters and argTypes`() {
		val args = arrayOf(3)
		val argTypes = intArrayOf(JDBCType.INTEGER.vendorTypeNumber)
		every { template.queryForObject(sql, args, argTypes, any<Class<Int>>()) } returns 2
		assertEquals(2, template.queryForObject<Int>(sql, args, argTypes))
		verify { template.queryForObject(sql, args, argTypes, any<Class<Int>>()) }
	}

	@Test
	fun `queryForObject with reified type parameters and args`() {
		val args = arrayOf(3)
		every { template.queryForObject(sql, args, any<Class<Int>>()) } returns 2
		assertEquals(2, template.queryForObject<Int>(sql, args))
		verify { template.queryForObject(sql, args, any<Class<Int>>()) }
	}

	@Test
	fun `queryForList with reified type parameters`() {
		val list = listOf(1, 2, 3)
		every { template.queryForList(sql, any<Class<Int>>()) } returns list
		assertEquals(list, template.queryForList<Int>(sql))
		verify { template.queryForList(sql, any<Class<Int>>()) }
	}

	@Test
	fun `queryForList with reified type parameters and argTypes`() {
		val list = listOf(1, 2, 3)
		val args = arrayOf(3)
		val argTypes = intArrayOf(JDBCType.INTEGER.vendorTypeNumber)
		every { template.queryForList(sql, args, argTypes, any<Class<Int>>()) } returns list
		assertEquals(list, template.queryForList<Int>(sql, args, argTypes))
		verify { template.queryForList(sql, args, argTypes, any<Class<Int>>()) }
	}

	@Test
	fun `queryForList with reified type parameters and args`() {
		val list = listOf(1, 2, 3)
		val args = arrayOf(3)
		every { template.queryForList(sql, args, any<Class<Int>>()) } returns list
		template.queryForList<Int>(sql, args)
		verify { template.queryForList(sql, args, any<Class<Int>>()) }
	}

	@Test
	fun `query with ResultSetExtractor-like function`() {
		every { template.query(eq(sql), any<ResultSetExtractor<Int>>(), eq(3)) } returns 2
		assertEquals(2, template.query<Int>(sql, 3) { rs ->
			rs.next()
			rs.getInt(1)
		})
		verify { template.query(eq(sql), any<ResultSetExtractor<Int>>(), eq(3)) }
	}

	@Test  // gh-22682
	fun `query with nullable ResultSetExtractor-like function`() {
		every { template.query(eq(sql), any<ResultSetExtractor<Int?>>(), eq(3)) } returns null
		assertNull(template.query<Int?>(sql, 3) { _ -> null })
		verify { template.query(eq(sql), any<ResultSetExtractor<Int?>>(), eq(3)) }
	}

	@Suppress("RemoveExplicitTypeArguments")
	@Test
	fun `query with RowCallbackHandler-like function`() {
		every { template.query(sql, ofType<RowCallbackHandler>(), 3) } returns Unit
		template.query(sql, 3) { rs ->
			assertEquals(22, rs.getInt(1))
		}
		verify { template.query(sql, ofType<RowCallbackHandler>(), 3) }
	}

	@Test
	fun `query with RowMapper-like function`() {
		val list = listOf(1, 2, 3)
		every { template.query(sql, ofType<RowMapper<*>>(), 3) } returns list
		assertEquals(list, template.query(sql, 3) { rs, _ ->
			rs.getInt(1)
		})
		verify { template.query(sql, ofType<RowMapper<*>>(), 3) }
	}

}
