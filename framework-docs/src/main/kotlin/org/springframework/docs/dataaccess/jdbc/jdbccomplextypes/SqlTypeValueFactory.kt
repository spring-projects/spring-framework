/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.dataaccess.jdbc.jdbccomplextypes

import oracle.jdbc.driver.OracleConnection
import org.springframework.jdbc.core.SqlTypeValue
import org.springframework.jdbc.core.support.AbstractSqlTypeValue
import java.sql.Connection
import java.sql.Date
import java.text.SimpleDateFormat

@Suppress("unused")
class SqlTypeValueFactory {

	fun createStructSample(): AbstractSqlTypeValue {
		// tag::struct[]
		val testItem = TestItem(123L, "A test item",
			SimpleDateFormat("yyyy-M-d").parse("2010-12-31"))

		val value = object : AbstractSqlTypeValue() {
			override fun createTypeValue(connection: Connection, sqlType: Int, typeName: String?): Any {
				val item = arrayOf<Any>(testItem.id, testItem.description,
					Date(testItem.expirationDate.time))
				return connection.createStruct(typeName, item)
			}
		}
		// end::struct[]
		return value
	}

	fun createOracleArray() : SqlTypeValue {
		// tag::oracle-array[]
		val ids = arrayOf(1L, 2L)
		val value: SqlTypeValue = object : AbstractSqlTypeValue() {
			override fun createTypeValue(conn: Connection, sqlType: Int, typeName: String?): Any {
				return conn.unwrap(OracleConnection::class.java).createOracleArray(typeName, ids)
			}
		}
		// end::oracle-array[]
		return value
	}

}