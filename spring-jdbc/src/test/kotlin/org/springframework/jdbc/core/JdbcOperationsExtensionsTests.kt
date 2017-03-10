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

package org.springframework.jdbc.core


import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.sql.*
import javax.sql.DataSource

/**
 * Mock object based tests for JdbcOperationsExtension
 */
class JdbcOperationsExtensionsTests {

	@Rule
	@JvmField
	val thrown = ExpectedException.none()!!

	lateinit private var connection: Connection
	lateinit private var dataSource: DataSource
	lateinit private var preparedStatement: PreparedStatement
	lateinit private var statement: Statement
	lateinit private var resultSet: ResultSet
	lateinit private var template: JdbcTemplate
	lateinit private var callableStatement: CallableStatement
	lateinit private var resultSetMetaData: ResultSetMetaData

	@Before
	fun setup() {
		connection = mock(Connection::class.java)
		dataSource = mock(DataSource::class.java)
		preparedStatement = mock(PreparedStatement::class.java)
		statement = mock(Statement::class.java)
		resultSet = mock(ResultSet::class.java)
		callableStatement = mock(CallableStatement::class.java)
		resultSetMetaData = mock(ResultSetMetaData::class.java)
		template = JdbcTemplate(dataSource)
		given(dataSource.connection).willReturn(connection)
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement)
		given(preparedStatement.executeQuery()).willReturn(resultSet)
		given(preparedStatement.executeQuery(anyString())).willReturn(resultSet)
		given(preparedStatement.connection).willReturn(connection)
		given(statement.connection).willReturn(connection)
		given(statement.executeQuery(anyString())).willReturn(resultSet)
		given(connection.prepareCall(anyString())).willReturn(callableStatement)
		given(connection.createStatement()).willReturn(statement)
		given(callableStatement.resultSet).willReturn(resultSet)
		given(resultSetMetaData.columnCount).willReturn(1)
		given(resultSetMetaData.getColumnName(1)).willReturn("age")
		given(resultSet.metaData).willReturn(resultSetMetaData)
		given(resultSet.next()).willReturn(true, false)
		given(resultSet.getInt(1)).willReturn(22)
	}


	@Test
	fun `queryForObject with KClass`() {
		val i = template.queryForObject("select age from customer where id = 3", Int::class)
		assertEquals(22, i)
	}

	@Test
	fun `queryForObject with reified type`() {
		val i: Int = template.queryForObject("select age from customer where id = 3")
		assertEquals(22, i)
	}

	@Test
	fun `queryForObject with RowMapper-like function`() {
		val i = template.queryForObject("select age from customer where id = ?", 3) { rs, _ ->
			rs.getInt(1)
		}
		assertEquals(22, i)
	}

	@Test
	fun `queryForObject with argTypes`() {
		val i = template.queryForObject("select age from customer where id = ?", arrayOf(3),
				intArrayOf(JDBCType.INTEGER.vendorTypeNumber), Int::class)
		assertEquals(22, i)
	}

	@Test
	fun `queryForObject with reified type and argTypes`() {
		val i: Int = template.queryForObject("select age from customer where id = ?", arrayOf(3),
				intArrayOf(JDBCType.INTEGER.vendorTypeNumber))
		assertEquals(22, i)
	}

	@Test
	fun `queryForObject with args`() {
		val i = template.queryForObject("select age from customer where id = ?", arrayOf(3), Int::class)
		assertEquals(22, i)
	}

	@Test
	fun `queryForObject with reified type and args`() {
		val i: Int = template.queryForObject("select age from customer where id = ?", arrayOf(3))
		assertEquals(22, i)
	}

	@Test
	fun `queryForObject with varargs`() {
		val i = template.queryForObject("select age from customer where id = ?", Int::class, 3)
		assertEquals(22, i)
	}

	@Test
	fun `queryForList with KClass`() {
		val i = template.queryForList(sql = "select age from customer where id = 3", elementType = Int::class)
		assertEquals(22, i.first())
	}

	@Test
	fun `queryForList with reified type`() {
		val i = template.queryForList<Int>("select age from customer where id = 3")
		assertEquals(22, i.first())
	}

	@Test
	fun `queryForList with argTypes`() {
		val i = template.queryForList(sql = "select age from customer where id = ?", args = arrayOf(3),
				argTypes = intArrayOf(JDBCType.INTEGER.vendorTypeNumber), elementType = Int::class)
		assertEquals(22, i.first())
	}

	@Test
	fun `queryForList with reified type and argTypes`() {
		val i = template.queryForList<Int>("select age from customer where id = ?", arrayOf(3), intArrayOf(JDBCType.INTEGER.vendorTypeNumber))
		assertEquals(22, i.first())
	}

	@Test
	fun `queryForList with args`() {
		val i = template.queryForList(sql = "select age from customer where id = ?", args = arrayOf(3), elementType = Int::class)
		assertEquals(22, i.first())
	}

	@Test
	fun `queryForList with reified type and args`() {
		val i = template.queryForList<Int>("select age from customer where id = ?", arrayOf(3))
		assertEquals(22, i.first())
	}

	@Test
	fun `query with ResultSetExtractor-like function`() {
		val i = template.query<Int>("select age from customer where id = ?", 3) { rs ->
			rs.next()
			rs.getInt(1)
		}
		assertEquals(22, i)
	}

	@Test
	fun `query with RowCallbackHandler-like function`() {
		template.query("select age from customer where id = ?", 3) { rs ->
			assertEquals(22, rs.getInt(1))
		}
	}

	@Test
	fun `query with RowMapper-like function`() {
		val i = template.query("select age from customer where id = ?", 3) { rs, _ ->
			rs.getInt(1)
		}
		assertEquals(22, i.first())
	}
}