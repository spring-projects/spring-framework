/*
 * Copyright 2002-present the original author or authors.
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

import org.springframework.dao.DataAccessResourceFailureException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests that a failed identity-column allocation does not publish a zero cache.
 *
 * @author Burak KALAYCI
 */
class AbstractIdentityColumnMaxValueIncrementerTests {

	@Test
	void retriesAllocationAfterInsertFailure() throws SQLException {
		DataSource dataSource = mock();
		Connection connection = mock();
		Statement statement = mock();
		ResultSet resultSet = mock();

		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeUpdate("insert into myseq values(null)"))
				.willThrow(new SQLException("insert failed"))
				.willReturn(1);
		given(statement.executeQuery("select max(identity()) from myseq")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(7L);

		HsqlMaxValueIncrementer incrementer = new HsqlMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setColumnName("seq");
		incrementer.afterPropertiesSet();

		assertThatThrownBy(incrementer::nextLongValue)
				.isInstanceOf(DataAccessResourceFailureException.class)
				.hasCauseInstanceOf(SQLException.class);

		assertThat(incrementer.nextLongValue()).isEqualTo(7L);
		verify(statement, times(2)).executeUpdate("insert into myseq values(null)");
	}

	@Test
	void doesNotServePartialCacheWhenLaterInsertFails() throws SQLException {
		DataSource dataSource = mock();
		Connection connection = mock();
		Statement statement = mock();
		ResultSet resultSet = mock();

		given(dataSource.getConnection()).willReturn(connection);
		given(connection.createStatement()).willReturn(statement);
		given(statement.executeUpdate("insert into myseq values(null)"))
				.willReturn(1)
				.willThrow(new SQLException("second insert failed"))
				.willReturn(1, 1);
		given(statement.executeQuery("select max(identity()) from myseq")).willReturn(resultSet);
		given(resultSet.next()).willReturn(true);
		given(resultSet.getLong(1)).willReturn(4L, 8L, 9L);

		HsqlMaxValueIncrementer incrementer = new HsqlMaxValueIncrementer();
		incrementer.setDataSource(dataSource);
		incrementer.setIncrementerName("myseq");
		incrementer.setColumnName("seq");
		incrementer.setCacheSize(2);
		incrementer.afterPropertiesSet();

		assertThatThrownBy(incrementer::nextLongValue)
				.isInstanceOf(DataAccessResourceFailureException.class);

		assertThat(incrementer.nextLongValue()).isEqualTo(8L);
		assertThat(incrementer.nextLongValue()).isEqualTo(9L);
		verify(statement, times(4)).executeUpdate("insert into myseq values(null)");
		verify(statement).executeUpdate("delete from myseq where seq < 9");
	}

}
