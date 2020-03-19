/*
 * Copyright 2002-2019 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for SQLErrorCodes loading.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @author Stephane Nicoll
 */
public class SQLErrorCodesFactoryTests {

	/**
	 * Check that a default instance returns empty error codes for an unknown database.
	 */
	@Test
	public void testDefaultInstanceWithNoSuchDatabase() {
		SQLErrorCodes sec = SQLErrorCodesFactory.getInstance().getErrorCodes("xx");
		assertThat(sec.getBadSqlGrammarCodes().length == 0).isTrue();
		assertThat(sec.getDataIntegrityViolationCodes().length == 0).isTrue();
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
		assertThat(sec.getBadSqlGrammarCodes().length > 0).isTrue();
		assertThat(sec.getDataIntegrityViolationCodes().length > 0).isTrue();
		// These had better be a Bad SQL Grammar code
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "942") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "6550") >= 0).isTrue();
		// This had better NOT be
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "9xx42") >= 0).isFalse();
	}

	private void assertIsSQLServer(SQLErrorCodes sec) {
		assertThat(sec.getDatabaseProductName()).isEqualTo("Microsoft SQL Server");

		assertThat(sec.getBadSqlGrammarCodes().length > 0).isTrue();

		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "156") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "170") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "207") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "208") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "209") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "9xx42") >= 0).isFalse();

		assertThat(sec.getPermissionDeniedCodes().length > 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getPermissionDeniedCodes(), "229") >= 0).isTrue();

		assertThat(sec.getDuplicateKeyCodes().length > 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDuplicateKeyCodes(), "2601") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDuplicateKeyCodes(), "2627") >= 0).isTrue();

		assertThat(sec.getDataIntegrityViolationCodes().length > 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "544") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "8114") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "8115") >= 0).isTrue();

		assertThat(sec.getDataAccessResourceFailureCodes().length > 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDataAccessResourceFailureCodes(), "4060") >= 0).isTrue();

		assertThat(sec.getCannotAcquireLockCodes().length > 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getCannotAcquireLockCodes(), "1222") >= 0).isTrue();

		assertThat(sec.getDeadlockLoserCodes().length > 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDeadlockLoserCodes(), "1205") >= 0).isTrue();
	}

	private void assertIsHsql(SQLErrorCodes sec) {
		assertThat(sec.getBadSqlGrammarCodes().length > 0).isTrue();
		assertThat(sec.getDataIntegrityViolationCodes().length > 0).isTrue();
		// This had better be a Bad SQL Grammar code
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "-22") >= 0).isTrue();
		// This had better NOT be
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "-9") >= 0).isFalse();
	}

	private void assertIsDB2(SQLErrorCodes sec) {
		assertThat(sec.getBadSqlGrammarCodes().length > 0).isTrue();
		assertThat(sec.getDataIntegrityViolationCodes().length > 0).isTrue();

		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "942") >= 0).isFalse();
		// This had better NOT be
		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "-204") >= 0).isTrue();
	}

	private void assertIsHana(SQLErrorCodes sec) {
		assertThat(sec.getBadSqlGrammarCodes().length > 0).isTrue();
		assertThat(sec.getDataIntegrityViolationCodes().length > 0).isTrue();

		assertThat(Arrays.binarySearch(sec.getBadSqlGrammarCodes(), "368") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getPermissionDeniedCodes(), "10") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDuplicateKeyCodes(), "301") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDataIntegrityViolationCodes(), "461") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDataAccessResourceFailureCodes(), "-813") >=0).isTrue();
		assertThat(Arrays.binarySearch(sec.getInvalidResultSetAccessCodes(), "582") >=0).isTrue();
		assertThat(Arrays.binarySearch(sec.getCannotAcquireLockCodes(), "131") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getCannotSerializeTransactionCodes(), "138") >= 0).isTrue();
		assertThat(Arrays.binarySearch(sec.getDeadlockLoserCodes(), "133") >= 0).isTrue();

	}

	@Test
	public void testLookupOrder() {
		class TestSQLErrorCodesFactory extends SQLErrorCodesFactory {
			private int lookups = 0;
			@Override
			protected Resource loadResource(String path) {
				++lookups;
				if (lookups == 1) {
					assertThat(path).isEqualTo(SQLErrorCodesFactory.SQL_ERROR_CODE_DEFAULT_PATH);
					return null;
				}
				else {
					// Should have only one more lookup
					assertThat(lookups).isEqualTo(2);
					assertThat(path).isEqualTo(SQLErrorCodesFactory.SQL_ERROR_CODE_OVERRIDE_PATH);
					return null;
				}
			}
		}

		// Should have failed to load without error
		TestSQLErrorCodesFactory sf = new TestSQLErrorCodesFactory();
		assertThat(sf.getErrorCodes("XX").getBadSqlGrammarCodes().length == 0).isTrue();
		assertThat(sf.getErrorCodes("Oracle").getDataIntegrityViolationCodes().length == 0).isTrue();
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
		assertThat(sf.getErrorCodes("XX").getBadSqlGrammarCodes().length == 0).isTrue();
		assertThat(sf.getErrorCodes("Oracle").getBadSqlGrammarCodes().length).isEqualTo(2);
		assertThat(sf.getErrorCodes("Oracle").getBadSqlGrammarCodes()[0]).isEqualTo("1");
		assertThat(sf.getErrorCodes("Oracle").getBadSqlGrammarCodes()[1]).isEqualTo("2");
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
		assertThat(sf.getErrorCodes("XX").getBadSqlGrammarCodes().length == 0).isTrue();
		assertThat(sf.getErrorCodes("Oracle").getBadSqlGrammarCodes().length).isEqualTo(0);
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
		assertThat(sf.getErrorCodes("Oracle").getCustomTranslations().length).isEqualTo(1);
		CustomSQLErrorCodesTranslation translation =
				sf.getErrorCodes("Oracle").getCustomTranslations()[0];
		assertThat(translation.getExceptionClass()).isEqualTo(CustomErrorCodeException.class);
		assertThat(translation.getErrorCodes().length).isEqualTo(1);
	}

	@Test
	public void testDataSourceWithNullMetadata() throws Exception {
		Connection connection = mock(Connection.class);
		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);

		SQLErrorCodes sec = SQLErrorCodesFactory.getInstance().getErrorCodes(dataSource);
		assertIsEmpty(sec);

		verify(connection).close();
	}

	@Test
	public void testGetFromDataSourceWithSQLException() throws Exception {
		SQLException expectedSQLException = new SQLException();

		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willThrow(expectedSQLException);

		SQLErrorCodes sec = SQLErrorCodesFactory.getInstance().getErrorCodes(dataSource);
		assertIsEmpty(sec);
	}

	private void assertIsEmpty(SQLErrorCodes sec) {
		// Codes should be empty
		assertThat(sec.getBadSqlGrammarCodes().length).isEqualTo(0);
		assertThat(sec.getDataIntegrityViolationCodes().length).isEqualTo(0);
	}

	private SQLErrorCodes getErrorCodesFromDataSource(String productName, SQLErrorCodesFactory factory) throws Exception {
		DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);
		given(databaseMetaData.getDatabaseProductName()).willReturn(productName);

		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(databaseMetaData);

		DataSource dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);

		SQLErrorCodesFactory secf = null;
		if (factory != null) {
			secf = factory;
		}
		else {
			secf = SQLErrorCodesFactory.getInstance();
		}

		SQLErrorCodes sec = secf.getErrorCodes(dataSource);


		SQLErrorCodes sec2 = secf.getErrorCodes(dataSource);
		assertThat(sec).as("Cached per DataSource").isSameAs(sec2);

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

}
