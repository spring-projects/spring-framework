/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.support.incrementer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link DataFieldMaxValueIncrementer} implementations.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mahmoud Ben Hassine
 * @since 27.02.2004
 */
class DataFieldMaxValueIncrementerTests {

	private final DataSource dataSource = mock();

	private final Connection connection = mock();

	private final Statement statement = mock();

	private final ResultSet resultSet = mock();


	@Test
	void hanaSequenceMaxValueIncrementer() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select myseq.nextval from dummy")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(10L, 12L);

		HanaSequenceMaxValueIncrementer incrementer = new HanaSequenceMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setPaddingLength(2);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextLongValue()).isEqualTo(10);
		assertThat(incrementer.nextStringValue()).isEqualTo("12");

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	void hsqlMaxValueIncrementer() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select max(identity()) from myseq")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(0L, 1L, 2L, 3L, 4L, 5L);

		HsqlMaxValueIncrementer incrementer = new HsqlMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setColumnName("seq");
		incrementer.setCacheSize(3);
		incrementer.setPaddingLength(3);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextIntValue()).isEqualTo(0);
		assertThat(incrementer.nextLongValue()).isEqualTo(1);
		assertThat(incrementer.nextStringValue()).isEqualTo("002");
		assertThat(incrementer.nextIntValue()).isEqualTo(3);
		assertThat(incrementer.nextLongValue()).isEqualTo(4);

		verify(statement, times(6)).executeUpdate("insert into myseq values(null)");
		verify(statement).executeUpdate("delete from myseq where seq < 2");
		verify(statement).executeUpdate("delete from myseq where seq < 5");
		verify(resultSet, times(6)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	void hsqlMaxValueIncrementerWithDeleteSpecificValues() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select max(identity()) from myseq")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(0L, 1L, 2L, 3L, 4L, 5L);

		HsqlMaxValueIncrementer incrementer = new HsqlMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setColumnName("seq");
		incrementer.setCacheSize(3);
		incrementer.setPaddingLength(3);
		incrementer.setDeleteSpecificValues(true);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextIntValue()).isEqualTo(0);
		assertThat(incrementer.nextLongValue()).isEqualTo(1);
		assertThat(incrementer.nextStringValue()).isEqualTo("002");
		assertThat(incrementer.nextIntValue()).isEqualTo(3);
		assertThat(incrementer.nextLongValue()).isEqualTo(4);

		verify(statement, times(6)).executeUpdate("insert into myseq values(null)");
		verify(statement).executeUpdate("delete from myseq where seq in (-1, 0, 1)");
		verify(statement).executeUpdate("delete from myseq where seq in (2, 3, 4)");
		verify(resultSet, times(6)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	void mySQLMaxValueIncrementer() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select last_insert_id()")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(2L, 4L);

		MySQLMaxValueIncrementer incrementer = new MySQLMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setColumnName("seq");
		incrementer.setCacheSize(2);
		incrementer.setPaddingLength(1);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextIntValue()).isEqualTo(1);
		assertThat(incrementer.nextLongValue()).isEqualTo(2);
		assertThat(incrementer.nextStringValue()).isEqualTo("3");
		assertThat(incrementer.nextLongValue()).isEqualTo(4);

		verify(statement, times(2)).executeUpdate("update myseq set seq = last_insert_id(seq + 2) limit 1");
		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	void mariaDBSequenceMaxValueIncrementer() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select next value for myseq")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(10L, 12L);

		MariaDBSequenceMaxValueIncrementer incrementer = new MariaDBSequenceMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setPaddingLength(5);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextStringValue()).isEqualTo("00010");
		assertThat(incrementer.nextIntValue()).isEqualTo(12);

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	void oracleSequenceMaxValueIncrementer() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select myseq.nextval from dual")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(10L, 12L);

		OracleSequenceMaxValueIncrementer incrementer = new OracleSequenceMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setPaddingLength(2);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextLongValue()).isEqualTo(10);
		assertThat(incrementer.nextStringValue()).isEqualTo("12");

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	void postgresSequenceMaxValueIncrementer() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select nextval('myseq')")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(10L, 12L);

		PostgresSequenceMaxValueIncrementer incrementer = new PostgresSequenceMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setPaddingLength(5);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextStringValue()).isEqualTo("00010");
		assertThat(incrementer.nextIntValue()).isEqualTo(12);

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	void sqlServerSequenceMaxValueIncrementer() throws SQLException {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeQuery("select next value for myseq")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(10L, 12L);

		SqlServerSequenceMaxValueIncrementer incrementer = new SqlServerSequenceMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setPaddingLength(5);
		incrementer.afterPropertiesSet();

		assertThat(incrementer.nextStringValue()).isEqualTo("00010");
		assertThat(incrementer.nextIntValue()).isEqualTo(12);

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

}
