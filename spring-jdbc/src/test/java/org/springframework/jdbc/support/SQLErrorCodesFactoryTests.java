/*
 * Copyright 2002-2020 the original author or authors.
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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Arrays;

import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Tests for SQLErrorCodes loading.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 */
public class SQLErrorCodesFactoryTests {

	/**
	 * Check that a default instance returns empty error codes for an unknown database.
	 */
	@Test
	public void testDefaultInstanceWithNoSuchDatabase() {
		SQLErrorCodes sec = SQLErrorCodesFactory.getInstance().getErrorCodes("xx");
		assertTrue(sec.getBadSqlGrammarCodes().length == 0);
		assertTrue(sec.getDataIntegrityViolationCodes().length == 0);
	}

	/**
	 * Check that a known database produces recognizable codes.
	 */
	@Test
	public void testDefaultInstanceWithOracle() {
		SQLErrorCodes sec = SQLErrorCodesFactory.getInstance().getErrorCodes("Oracle");
		assertIsOracle(sec);
	}

	private void assertIsOracle(SQLErrorCodes sec) {
		assertTrue(sec.getBadSqlGrammarCodes().length > 0);
		assertTrue(sec.getDataIntegrityViolationCodes().length > 0);
		// These had better be a Bad SQL Grammar code
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "942") >= 0);
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "6550") >= 0);
		// This had better NOT be
		assertFalse(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "9xx42") >= 0);
	}

	private void assertIsSQLServer(SQLErrorCodes sec) {
		assertThat(sec.getDatabaseProductName(), equalTo("Microsoft SQL Server"));

		assertTrue(sec.getBadSqlGrammarCodes().length > 0);

		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "156") >= 0);
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "170") >= 0);
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "207") >= 0);
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "208") >= 0);
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "209") >= 0);
		assertFalse(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "9xx42") >= 0);

		assertTrue(sec.getPermissionDeniedCodes().length > 0);
		assertTrue(Arrays.binarySearch(sec.getPermissionDeniedCodes(), "229") >= 0);

		assertTrue(sec.getDuplicateKeyCodes().length > 0);
		assertTrue(Arrays.binarySearch(sec.getDuplicateKeyCodes(), "2601") >= 0);
		assertTrue(Arrays.binarySearch(sec.getDuplicateKeyCodes(), "2627") >= 0);

		assertTrue(sec.getDataIntegrityViolationCodes().length > 0);
		assertTrue(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "544") >= 0);
		assertTrue(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "8114") >= 0);
		assertTrue(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "8115") >= 0);

		assertTrue(sec.getDataAccessResourceFailureCodes().length > 0);
		assertTrue(Arrays.binarySearch(sec.getDataAccessResourceFailureCodes(), "4060") >= 0);

		assertTrue(sec.getCannotAcquireLockCodes().length > 0);
		assertTrue(Arrays.binarySearch(sec.getCannotAcquireLockCodes(), "1222") >= 0);

		assertTrue(sec.getDeadlockLoserCodes().length > 0);
		assertTrue(Arrays.binarySearch(sec.getDeadlockLoserCodes(), "1205") >= 0);
	}

	private void assertIsHsql(SQLErrorCodes sec) {
		assertTrue(sec.getBadSqlGrammarCodes().length > 0);
		assertTrue(sec.getDataIntegrityViolationCodes().length > 0);
		// This had better be a Bad SQL Grammar code
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "-22") >= 0);
		// This had better NOT be
		assertFalse(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "-9") >= 0);
	}

	private void assertIsDB2(SQLErrorCodes sec) {
		assertTrue(sec.getBadSqlGrammarCodes().length > 0);
		assertTrue(sec.getDataIntegrityViolationCodes().length > 0);

		assertFalse(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "942") >= 0);
		// This had better NOT be
		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "-204") >= 0);
	}

	private void assertIsHana(SQLErrorCodes sec) {
		assertTrue(sec.getBadSqlGrammarCodes().length > 0);
		assertTrue(sec.getDataIntegrityViolationCodes().length > 0);

		assertTrue(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "368") >= 0);
		assertTrue(Arrays.binarySearch(sec.getPermissionDeniedCodes(), "10") >= 0);
		assertTrue(Arrays.binarySearch(sec.getDuplicateKeyCodes(), "301") >= 0);
		assertTrue(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "461") >= 0);
		assertTrue(Arrays.binarySearch(sec.getDataAccessResourceFailureCodes(), "-813") >=0);
		assertTrue(Arrays.binarySearch(sec.getInvalidResultSetAccessCodes(), "582") >=0);
		assertTrue(Arrays.binarySearch(sec.getCannotAcquireLockCodes(), "131") >= 0);
		assertTrue(Arrays.binarySearch(sec.getCannotSerializeTransactionCodes(), "138") >= 0);
		assertTrue(Arrays.binarySearch(sec.getDeadlockLoserCodes(), "133") >= 0);

	}

	@Test
	public void testLookupOrder() {
		class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
			private int lookups = 0;
			@Override
			protected Resource loadResource(String path) {
				++lookups;
				if (lookups == 1) {
					assertEquals(SQLErrorCodesFactory.SQL_ERROR_CODE_DEFAULT_PATH, path);
					return null;
				}
				else {
					// Should have only one more lookup
					assertEquals(2, lookups);
					assertEquals(SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH, path);
					return null;
				}
			}
		}

		// Should have failed to load without error
		TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
		assertTrue(sf.getErrorCodes("XX").getBadSqlGrammarCodes().length == 0);
		assertTrue(sf.getErrorCodes("Oracle").getDataIntegrityViolationCodes().length == 0);
	}

	/**
	 * Check that user defined error codes take precedence.
	 */
	@Test
	public void testFindUserDefinedCodes() {
		class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
			@Override
			protected Resource loadResource(String path) {
				if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
					return new ClassPathResource("test-error-codes.xml", SQLErrorCodesFactoryTests.class);
				}
				return null;
			}
		}

		// Should have loaded without error
		TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
		assertTrue(sf.getErrorCodes("XX").getBadSqlGrammarCodes().length == 0);
		assertEquals(2, sf.getErrorCodes("Oracle").getBadSqlGrammarCodes().length);
		assertEquals("1", sf.getErrorCodes("Oracle").getBadSqlGrammarCodes()[0]);
		assertEquals("2", sf.getErrorCodes("Oracle").getBadSqlGrammarCodes()[1]);
	}

	@Test
	public void testInvalidUserDefinedCodeFormat() {
		class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
			@Override
			protected Resource loadResource(String path) {
				if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
					// Guaranteed to be on the classpath, but most certainly NOT XML
					return new ClassPathResource("SQLExceptionTranslator.class", SQLErrorCodesFactoryTests.class);
				}
				return null;
			}
		}

		// Should have failed to load without error
		TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
		assertTrue(sf.getErrorCodes("XX").getBadSqlGrammarCodes().length == 0);
		assertEquals(0, sf.getErrorCodes("Oracle").getBadSqlGrammarCodes().length);
	}

	/**
	 * Check that custom error codes take precedence.
	 */
	@Test
	public void testFindCustomCodes() {
		class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
			@Override
			protected Resource loadResource(String path) {
				if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
					return new ClassPathResource("custom-error-codes.xml", SQLErrorCodesFactoryTests.class);
				}
				return null;
			}
		}

		// Should have loaded without error
		TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
		assertEquals(1, sf.getErrorCodes("Oracle").getCustomTranslations().length);
		CustomSQLErrorCodesTranslation translation =
				sf.getErrorCodes("Oracle").getCustomTranslations()[0];
		assertEquals(CustomErrorCodeException.class, translation.getExceptionClass());
		assertEquals(1, translation.getErrorCodes().length);
	}

	@Test
	public void testDataSourceWithNullMetadata() throws Exception {
		Connection connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);

		SQLErrorCodes sec = SQLErrorCodesFactory.getInstance().getErrorCodes(dataSource);
		assertIsEmpty(sec);
		verify(connection).close();

		reset(connection);
		sec = SQLErrorCodesFactory.getInstance().resolveErrorCodes(dataSource);
		assertNull(sec);
		verify(connection).close();
	}

	@Test
	public void testGetFromDataSourceWithSQLException() throws Exception {
		SQLException expectedSQLException = new SQLException();

		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willThrow(expectedSQLException);

		SQLErrorCodes sec = SQLErrorCodesFactory.getInstance().getErrorCodes(dataSource);
		assertIsEmpty(sec);

		sec = SQLErrorCodesFactory.getInstance().resolveErrorCodes(dataSource);
		assertNull(sec);
	}

	private SQLErrorCodes getErrorCodesFromDataSource(String productName, SQLErrorCodesFactory factory) throws Exception {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(databaseMetaData.getDatabaseProductName()).willReturn(productName);

		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(databaseMetaData);

		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);

		SQLErrorCodesFactory secf = (factory != null ? factory : SQLErrorCodesFactory.getInstance());
		SQLErrorCodes sec = secf.getErrorCodes(dataSource);

		SQLErrorCodes sec2 = secf.getErrorCodes(dataSource);
		assertSame("Cached per DataSource", sec2, sec);

		verify(connection).close();
		return sec;
	}

	@Test
	public void testSQLServerRecognizedFromMetadata() throws Exception {
		SQLErrorCodes sec = getErrorCodesFromDataSource("MS-SQL", null);
		assertIsSQLServer(sec);
	}

	@Test
	public void testOracleRecognizedFromMetadata() throws Exception {
		SQLErrorCodes sec = getErrorCodesFromDataSource("Oracle", null);
		assertIsOracle(sec);
	}

	@Test
	public void testHsqlRecognizedFromMetadata() throws Exception {
		SQLErrorCodes sec = getErrorCodesFromDataSource("HSQL Database Engine", null);
		assertIsHsql(sec);
	}

	@Test
	public void testDB2RecognizedFromMetadata() throws Exception {
		SQLErrorCodes sec = getErrorCodesFromDataSource("DB2", null);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("DB2/", null);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("DB-2", null);
		assertIsEmpty(sec);
	}

	@Test
	public void testHanaIsRecognizedFromMetadata() throws Exception {
		SQLErrorCodes sec = getErrorCodesFromDataSource("SAP DB", null);
		assertIsHana(sec);
	}

	/**
	 * Check that wild card database name works.
	 */
	@Test
	public void testWildCardNameRecognized() throws Exception {
		class WildcardSQLErrorCodesFactory extends SQLErrorCodesFactory {
			@Override
			protected Resource loadResource(String path) {
				if (SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH.equals(path)) {
					return new ClassPathResource("wildcard-error-codes.xml", SQLErrorCodesFactoryTests.class);
				}
				return null;
			}
		}

		WildcardSQLErrorCodesFactory factory = new WildcardSQLErrorCodesFactory();
		SQLErrorCodes sec = getErrorCodesFromDataSource("DB2", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("DB2 UDB for Xxxxx", factory);
		assertIsDB2(sec);

		sec = getErrorCodesFromDataSource("DB3", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("DB3/", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("/DB3", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("/DB3", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("/DB3/", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("DB-3", factory);
		assertIsEmpty(sec);

		sec = getErrorCodesFromDataSource("DB1", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("DB1/", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("/DB1", factory);
		assertIsEmpty(sec);
		sec = getErrorCodesFromDataSource("/DB1/", factory);
		assertIsEmpty(sec);

		sec = getErrorCodesFromDataSource("DB0", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("/DB0", factory);
		assertIsDB2(sec);
		sec = getErrorCodesFromDataSource("DB0/", factory);
		assertIsEmpty(sec);
		sec = getErrorCodesFromDataSource("/DB0/", factory);
		assertIsEmpty(sec);
	}

	private void assertIsEmpty(SQLErrorCodes sec) {
		assertEquals(0, sec.getBadSqlGrammarCodes().length);
		assertEquals(0, sec.getDataIntegrityViolationCodes().length);
	}

}
