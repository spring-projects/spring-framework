/*
 * Copyright 2002-2017 the original author or authors
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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import java.sql.*

/**
 * Mock object based tests for [JdbcOperations] Kotlin extensions
 *
 * @author Mario Arias
 * @author Sebastien Deleuze
 */
@RunWith(MockitoJUnitRunner::class)
class JdbcOperationsExtensionsTests {

	@Mock(answer = Answers.RETURNS_MOCKS)
	lateinit var template: JdbcTemplate

	@Test
	fun `queryForObject with reified type parameters`() {
		val sql = "select age from customer where id = 3"
		template.queryForObject<Int>(sql)
		verify(template, times(1)).queryForObject(sql, Integer::class.java)
	}

	@Test
	fun `queryForObject with RowMapper-like function`() {
		val sql = "select age from customer where id = ?"
		template.queryForObject(sql, 3) { rs: ResultSet, _: Int -> rs.getInt(1) }
		verify(template, times(1)).queryForObject(eq(sql), any<RowMapper<Int>>(), eq(3))
	}

	@Test
	fun `queryForObject with reified type parameters and argTypes`() {
		val sql = "select age from customer where id = ?"
		val args = arrayOf(3)
		val argTypes = intArrayOf(JDBCType.INTEGER.vendorTypeNumber)
		template.queryForObject<Int>(sql, args, argTypes)
		verify(template, times(1)).queryForObject(sql, args, argTypes, Integer::class.java)
	}

	@Test
	fun `queryForObject with reified type parameters and args`() {
		val sql = "select age from customer where id = ?"
		val args = arrayOf(3)
		template.queryForObject<Int>(sql, args)
		verify(template, times(1)).queryForObject(sql, args, Integer::class.java)
	}

	@Test
	fun `queryForList with reified type parameters`() {
		val sql = "select age from customer where id = 3"
		template.queryForList<Int>(sql)
		verify(template, times(1)).queryForList(sql, Integer::class.java)
	}

	@Test
	fun `queryForList with reified type parameters and argTypes`() {
		val sql = "select age from customer where id = ?"
		val args = arrayOf(3)
		val argTypes = intArrayOf(JDBCType.INTEGER.vendorTypeNumber)
		template.queryForList<Int>(sql, args, argTypes)
		verify(template, times(1)).queryForList(sql, args, argTypes, Integer::class.java)
	}

	@Test
	fun `queryForList with reified type parameters and args`() {
		val sql = "select age from customer where id = ?"
		val args = arrayOf(3)
		template.queryForList<Int>(sql, args)
		verify(template, times(1)).queryForList(sql, args, Integer::class.java)
	}

	@Test
	fun `query with ResultSetExtractor-like function`() {
		val sql = "select age from customer where id = ?"
		template.query<Int>(sql, 3) { rs ->
			rs.next()
			rs.getInt(1)
		}
		verify(template, times(1)).query(eq(sql), any<ResultSetExtractor<Int>>(), eq(3))
	}

	@Test
	fun `query with RowCallbackHandler-like function`() {
		val sql = "select age from customer where id = ?"
		template.query(sql, 3) { rs ->
			assertEquals(22, rs.getInt(1))
		}
		verify(template, times(1)).query(eq(sql), any<RowCallbackHandler>(), eq(3))
	}

	@Test
	fun `query with RowMapper-like function`() {
		val sql = "select age from customer where id = ?"
		template.query(sql, 3) { rs, _ ->
			rs.getInt(1)
		}
		verify(template, times(1)).query(eq(sql), any<RowMapper<Int>>(), eq(3))
	}

}
