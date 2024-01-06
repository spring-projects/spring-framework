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

package org.springframework.jdbc.core;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.IncorrectResultSizeDataAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Rob Winch
 * @since 19.12.2004
 */
class JdbcTemplateQueryTests {

	private DataSource dataSource = mock();

	private Connection connection = mock();

	private Statement statement = mock();

	private PreparedStatement preparedStatement = mock();

	private ResultSet resultSet = mock();

	private ResultSetMetaData resultSetMetaData = mock();

	private JdbcTemplate template = new JdbcTemplate(this.dataSource);


	@BeforeEach
	void setup() throws Exception {
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.connection.createStatement()).willReturn(this.statement);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(this.statement.getConnection()).willReturn(this.connection);
		given(this.statement.executeQuery(anyString())).willReturn(this.resultSet);
		given(this.preparedStatement.getConnection()).willReturn(this.connection);
		given(this.preparedStatement.executeQuery()).willReturn(this.resultSet);
		given(this.resultSet.getMetaData()).willReturn(this.resultSetMetaData);
		given(this.resultSetMetaData.getColumnCount()).willReturn(1);
		given(this.resultSetMetaData.getColumnLabel(1)).willReturn("age");
	}


	@Test
	void testQueryForList() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getObject(1)).willReturn(11, 12);
		List<Map<String, Object>> li = this.template.queryForList(sql);
		assertThat(li).as("All rows returned").hasSize(2);
		assertThat(((Integer) li.get(0).get("age"))).as("First row is Integer").isEqualTo(11);
		assertThat(((Integer) li.get(1).get("age"))).as("Second row is Integer").isEqualTo(12);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForListWithEmptyResult() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(false);
		List<Map<String, Object>> li = this.template.queryForList(sql);
		assertThat(li).as("All rows returned").isEmpty();
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForListWithSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		List<Map<String, Object>> li = this.template.queryForList(sql);
		assertThat(li).as("All rows returned").hasSize(1);
		assertThat(((Integer) li.get(0).get("age"))).as("First row is Integer").isEqualTo(11);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForListWithIntegerElement() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(11);
		List<Integer> li = this.template.queryForList(sql, Integer.class);
		assertThat(li).containsExactly(11);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForMapWithSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		Map<String, Object> map = this.template.queryForMap(sql);
		assertThat((Integer) map.get("age")).as("Wow is Integer").isEqualTo(11);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectThrowsIncorrectResultSizeForMoreThanOneRow() throws Exception {
		String sql = "select pass from t_account where first_name='Alef'";
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getString(1)).willReturn("pass");
		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				this.template.queryForObject(sql, String.class));
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithRowMapper() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		Object o = this.template.queryForObject(sql, (rs, rowNum) -> rs.getInt(1));
		assertThat(o).as("Correct result type").isInstanceOf(Integer.class);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForStreamWithRowMapper() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		AtomicInteger count = new AtomicInteger();
		try (Stream<Integer> s = this.template.queryForStream(sql, (rs, rowNum) -> rs.getInt(1))) {
			s.forEach(val -> {
				count.incrementAndGet();
				assertThat(val).isEqualTo(22);
			});
		}
		assertThat(count.get()).isEqualTo(1);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithString() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getString(1)).willReturn("myvalue");
		assertThat(this.template.queryForObject(sql, String.class)).isEqualTo("myvalue");
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithBigInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1, BigInteger.class)).willReturn(new BigInteger("22"));
		assertThat(this.template.queryForObject(sql, BigInteger.class)).isEqualTo(new BigInteger("22"));
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithBigDecimal() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getBigDecimal(1)).willReturn(new BigDecimal("22.5"));
		assertThat(this.template.queryForObject(sql, BigDecimal.class)).isEqualTo(new BigDecimal("22.5"));
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		assertThat(this.template.queryForObject(sql, Integer.class)).isEqualTo(Integer.valueOf(22));
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithIntegerAndNull() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(0);
		given(this.resultSet.wasNull()).willReturn(true);
		assertThat(this.template.queryForObject(sql, Integer.class)).isNull();
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForInt() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		int i = this.template.queryForObject(sql, Integer.class);
		assertThat(i).as("Return of an int").isEqualTo(22);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForIntPrimitive() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		int i = this.template.queryForObject(sql, int.class);
		assertThat(i).as("Return of an int").isEqualTo(22);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForLong() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getLong(1)).willReturn(87L);
		long l = this.template.queryForObject(sql, Long.class);
		assertThat(l).as("Return of a long").isEqualTo(87);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForLongPrimitive() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = 3";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getLong(1)).willReturn(87L);
		long l = this.template.queryForObject(sql, long.class);
		assertThat(l).as("Return of a long").isEqualTo(87);
		verify(this.resultSet).close();
		verify(this.statement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForListWithArgs() throws Exception {
		doTestQueryForListWithArgs("SELECT AGE FROM CUSTMR WHERE ID < ?");
	}

	@Test
	void testQueryForListIsNotConfusedByNamedParameterPrefix() throws Exception {
		doTestQueryForListWithArgs("SELECT AGE FROM PREFIX:CUSTMR WHERE ID < ?");
	}

	private void doTestQueryForListWithArgs(String sql) throws Exception {
		given(this.resultSet.next()).willReturn(true, true, false);
		given(this.resultSet.getObject(1)).willReturn(11, 12);
		List<Map<String, Object>> li = this.template.queryForList(sql, 3);
		assertThat(li).as("All rows returned").hasSize(2);
		assertThat(((Integer) li.get(0).get("age"))).as("First row is Integer").isEqualTo(11);
		assertThat(((Integer) li.get(1).get("age"))).as("Second row is Integer").isEqualTo(12);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForListWithArgsAndEmptyResult() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(false);
		List<Map<String, Object>> li = this.template.queryForList(sql, 3);
		assertThat(li).as("All rows returned").isEmpty();
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForListWithArgsAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		List<Map<String, Object>> li = this.template.queryForList(sql, 3);
		assertThat(li).as("All rows returned").hasSize(1);
		assertThat(((Integer) li.get(0).get("age"))).as("First row is Integer").isEqualTo(11);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForListWithArgsAndIntegerElementAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(11);
		List<Integer> li = this.template.queryForList(sql, Integer.class, 3);
		assertThat(li).containsExactly(11);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForMapWithArgsAndSingleRowAndColumn() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID < ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getObject(1)).willReturn(11);
		Map<String, Object> map = this.template.queryForMap(sql, 3);
		assertThat(((Integer) map.get("age"))).as("Row is Integer").isEqualTo(11);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithArgsAndRowMapper() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		Object o = this.template.queryForObject(sql, (rs, rowNum) -> rs.getInt(1), 3);
		assertThat(o).as("Correct result type").isInstanceOf(Integer.class);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForStreamWithArgsAndRowMapper() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		AtomicInteger count = new AtomicInteger();
		try (Stream<Integer> s = this.template.queryForStream(sql, (rs, rowNum) -> rs.getInt(1), 3)) {
			s.forEach(val -> {
				count.incrementAndGet();
				assertThat(val).isEqualTo(22);
			});
		}
		assertThat(count.get()).isEqualTo(1);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForObjectWithArgsAndInteger() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		Object o = this.template.queryForObject(sql, Integer.class, 3);
		assertThat(o).as("Correct result type").isInstanceOf(Integer.class);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForIntWithArgs() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getInt(1)).willReturn(22);
		int i = this.template.queryForObject(sql, Integer.class, 3);
		assertThat(i).as("Return of an int").isEqualTo(22);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

	@Test
	void testQueryForLongWithArgs() throws Exception {
		String sql = "SELECT AGE FROM CUSTMR WHERE ID = ?";
		given(this.resultSet.next()).willReturn(true, false);
		given(this.resultSet.getLong(1)).willReturn(87L);
		long l = this.template.queryForObject(sql, Long.class, 3);
		assertThat(l).as("Return of a long").isEqualTo(87);
		verify(this.preparedStatement).setObject(1, 3);
		verify(this.resultSet).close();
		verify(this.preparedStatement).close();
		verify(this.connection).close();
	}

}
