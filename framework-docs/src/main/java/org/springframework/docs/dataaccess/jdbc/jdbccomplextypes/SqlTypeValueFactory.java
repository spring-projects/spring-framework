/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.docs.dataaccess.jdbc.jdbccomplextypes;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import oracle.jdbc.driver.OracleConnection;

import org.springframework.jdbc.core.SqlTypeValue;
import org.springframework.jdbc.core.support.AbstractSqlTypeValue;

@SuppressWarnings("unused")
class SqlTypeValueFactory {

	void createStructSample() throws ParseException {
		// tag::struct[]
		TestItem testItem = new TestItem(123L, "A test item",
				new SimpleDateFormat("yyyy-M-d").parse("2010-12-31"));

		SqlTypeValue value = new AbstractSqlTypeValue() {
			protected Object createTypeValue(Connection connection, int sqlType, String typeName) throws SQLException {
				Object[] item = new Object[] { testItem.getId(), testItem.getDescription(),
						new java.sql.Date(testItem.getExpirationDate().getTime()) };
				return connection.createStruct(typeName, item);
			}
		};
		// end::struct[]
	}

	void createOracleArray() {
		// tag::oracle-array[]
		Long[] ids = new Long[] {1L, 2L};

		SqlTypeValue value = new AbstractSqlTypeValue() {
			protected Object createTypeValue(Connection conn, int sqlType, String typeName) throws SQLException {
				return conn.unwrap(OracleConnection.class).createOracleArray(typeName, ids);
			}
		};
		// end::oracle-array[]
	}

}
