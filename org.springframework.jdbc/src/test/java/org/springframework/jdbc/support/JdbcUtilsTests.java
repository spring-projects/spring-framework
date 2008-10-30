/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.support;

import junit.framework.TestCase;

/**
 * Unit tests for JdbcUtils.
 *
 * @author Thomas Risberg
 */
public class JdbcUtilsTests extends TestCase {

	public void testCommonDatabaseName() {
		assertEquals("Wrong db name", "Oracle", JdbcUtils.commonDatabaseName("Oracle"));
		assertEquals("Wrong db name", "DB2", JdbcUtils.commonDatabaseName("DB2-for-Spring"));
		assertEquals("Wrong db name", "Sybase", JdbcUtils.commonDatabaseName("Sybase SQL Server"));
		assertEquals("Wrong db name", "Sybase", JdbcUtils.commonDatabaseName("Adaptive Server Enterprise"));
		assertEquals("Wrong db name", "MySQL", JdbcUtils.commonDatabaseName("MySQL"));
	}

	public void testConvertUnderscoreNameToPropertyName() {
		assertEquals("Wrong property name", "myName", JdbcUtils.convertUnderscoreNameToPropertyName("MY_NAME"));
		assertEquals("Wrong property name", "yourName", JdbcUtils.convertUnderscoreNameToPropertyName("yOUR_nAME"));
		assertEquals("Wrong property name", "AName", JdbcUtils.convertUnderscoreNameToPropertyName("a_name"));
		assertEquals("Wrong property name", "someoneElsesName", JdbcUtils.convertUnderscoreNameToPropertyName("someone_elses_name"));
	}

}
