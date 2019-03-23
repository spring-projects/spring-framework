/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.jdbc.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link JdbcUtils}.
 *
 * @author Thomas Risberg
 */
public class JdbcUtilsTests {

	@Test
	public void commonDatabaseName() {
		assertEquals("Oracle", JdbcUtils.commonDatabaseName("Oracle"));
		assertEquals("DB2", JdbcUtils.commonDatabaseName("DB2-for-Spring"));
		assertEquals("Sybase", JdbcUtils.commonDatabaseName("Sybase SQL Server"));
		assertEquals("Sybase", JdbcUtils.commonDatabaseName("Adaptive Server Enterprise"));
		assertEquals("MySQL", JdbcUtils.commonDatabaseName("MySQL"));
	}

	@Test
	public void convertUnderscoreNameToPropertyName() {
		assertEquals("myName", JdbcUtils.convertUnderscoreNameToPropertyName("MY_NAME"));
		assertEquals("yourName", JdbcUtils.convertUnderscoreNameToPropertyName("yOUR_nAME"));
		assertEquals("AName", JdbcUtils.convertUnderscoreNameToPropertyName("a_name"));
		assertEquals("someoneElsesName", JdbcUtils.convertUnderscoreNameToPropertyName("someone_elses_name"));
	}

}
