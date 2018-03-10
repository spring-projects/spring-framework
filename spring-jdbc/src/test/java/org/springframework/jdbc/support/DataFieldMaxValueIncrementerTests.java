/*
 * Copyright 2002-2018 the original author or authors.
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

import org.junit.Test;

import org.springframework.jdbc.support.incrementer.HanaSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.HsqlMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.MySQLMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.OracleSequenceMaxValueIncrementer;
import org.springframework.jdbc.support.incrementer.PostgresSequenceMaxValueIncrementer;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Juergen Hoeller
 * @since 27.02.2004
 */
public class DataFieldMaxValueIncrementerTests {

	private final DataSource dataSource = mock(DataSource.class);

	private final Connection connection = mock(Connection.class);

	private final Statement statement = mock(Statement.class);

	private final ResultSet resultSet = mock(ResultSet.class);


	@Test
	public void testHanaSequenceMaxValueIncrementer() throws SQLException {
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

		assertEquals(10, incrementer.nextLongValue());
		assertEquals("12", incrementer.nextStringValue());

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	public void testHsqlMaxValueIncrementer() throws SQLException {
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

		assertEquals(0, incrementer.nextIntValue());
		assertEquals(1, incrementer.nextLongValue());
		assertEquals("002", incrementer.nextStringValue());
		assertEquals(3, incrementer.nextIntValue());
		assertEquals(4, incrementer.nextLongValue());

		verify(statement, times(6)).executeUpdate("insert into myseq values(null)");
		verify(statement).executeUpdate("delete from myseq where seq < 2");
		verify(statement).executeUpdate("delete from myseq where seq < 5");
		verify(resultSet, times(6)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	public void testHsqlMaxValueIncrementerWithDeleteSpecificValues() throws SQLException {
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

		assertEquals(0, incrementer.nextIntValue());
		assertEquals(1, incrementer.nextLongValue());
		assertEquals("002", incrementer.nextStringValue());
		assertEquals(3, incrementer.nextIntValue());
		assertEquals(4, incrementer.nextLongValue());

		verify(statement, times(6)).executeUpdate("insert into myseq values(null)");
		verify(statement).executeUpdate("delete from myseq where seq in (-1, 0, 1)");
		verify(statement).executeUpdate("delete from myseq where seq in (2, 3, 4)");
		verify(resultSet, times(6)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	public void testMySQLMaxValueIncrementer() throws SQLException {
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

		assertEquals(1, incrementer.nextIntValue());
		assertEquals(2, incrementer.nextLongValue());
		assertEquals("3", incrementer.nextStringValue());
		assertEquals(4, incrementer.nextLongValue());

		verify(statement, times(2)).executeUpdate("update myseq set seq = last_insert_id(seq + 2)");
		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	public void testOracleSequenceMaxValueIncrementer() throws SQLException {
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

		assertEquals(10, incrementer.nextLongValue());
		assertEquals("12", incrementer.nextStringValue());

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

	@Test
	public void testPostgresSequenceMaxValueIncrementer() throws SQLException {
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

		assertEquals("00010", incrementer.nextStringValue());
		assertEquals(12, incrementer.nextIntValue());

		verify(resultSet, times(2)).close();
		verify(statement, times(2)).close();
		verify(connection, times(2)).close();
	}

}
