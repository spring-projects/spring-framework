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

package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import org.springframework.jdbc.Customer;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @author Yanming Zhou
 * @since 6.1
 */
class JdbcClientIndexedParameterTests {

	private static final String SELECT_INDEXED_PARAMETERS =
			"select id, forename from custmr where id = ? and country = ?";

	private static final String SELECT_NO_PARAMETERS =
			"select id, forename from custmr";

	private static final String UPDATE_INDEXED_PARAMETERS =
			"update seat_status set booking_id = null where performance_id = ? and price_band_id = ?";

	private static final String INSERT_GENERATE_KEYS =
			"insert into show (name) values(?)";

	private static final String[] COLUMN_NAMES = new String[] {"id", "forename"};


	private final Connection connection = mock();

	private final DataSource dataSource = mock();

	private final PreparedStatement preparedStatement = mock();

	private final ResultSet resultSet = mock();

	private final ResultSetMetaData resultSetMetaData = mock();

	private final DatabaseMetaData databaseMetaData = mock();

	private final JdbcClient client = JdbcClient.create(dataSource);

	private final List<Object> params = new ArrayList<>();


	@BeforeEach
	void setup() throws Exception {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
		given(preparedStatement.getConnection()).willReturn(connection);
		given(preparedStatement.executeQuery()).willReturn(resultSet);
		given(databaseMetaData.getDatabaseProductName()).willReturn("MySQL");
		given(databaseMetaData.supportsBatchUpdates()).willReturn(true);
	}


	@Test
	void queryWithResultSetExtractor() throws SQLException {
		given(resultSet.next()).willReturn(true);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		params.add(new SqlParameterValue(Types.DECIMAL, 1));
		params.add("UK");
		Customer cust = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
				rs -> {
					rs.next();
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				});

		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryWithResultSetExtractorNoParameters() throws SQLException {
		given(resultSet.next()).willReturn(true);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		Customer cust = client.sql(SELECT_NO_PARAMETERS).query(
				rs -> {
					rs.next();
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				});

		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryWithRowCallbackHandler() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		params.add(new SqlParameterValue(Types.DECIMAL, 1));
		params.add("UK");
		final List<Customer> customers = new ArrayList<>();
		client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(rs -> {
			Customer cust = new Customer();
			cust.setId(rs.getInt(COLUMN_NAMES[0]));
			cust.setForename(rs.getString(COLUMN_NAMES[1]));
			customers.add(cust);
		});

		assertThat(customers).hasSize(1);
		assertThat(customers.get(0).getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(customers.get(0).getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryWithRowCallbackHandlerNoParameters() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		final List<Customer> customers = new ArrayList<>();
		client.sql(SELECT_NO_PARAMETERS).query(rs -> {
			Customer cust = new Customer();
			cust.setId(rs.getInt(COLUMN_NAMES[0]));
			cust.setForename(rs.getString(COLUMN_NAMES[1]));
			customers.add(cust);
		});

		assertThat(customers).hasSize(1);
		assertThat(customers.get(0).getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(customers.get(0).getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryWithRowMapper() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		params.add(new SqlParameterValue(Types.DECIMAL, 1));
		params.add("UK");
		List<Customer> customers = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
				(rs, rownum) -> {
					Customer cust = new Customer();
					cust.setId(rs.getInt(COLUMN_NAMES[0]));
					cust.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust;
				}).list();

		assertThat(customers).hasSize(1);
		Customer cust = customers.get(0);
		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryWithRowMapperNoParameters() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		Set<Customer> customers = client.sql(SELECT_NO_PARAMETERS).query(
				(rs, rownum) -> {
					Customer cust = new Customer();
					cust.setId(rs.getInt(COLUMN_NAMES[0]));
					cust.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust;
				}).set();

		assertThat(customers).hasSize(1);
		Customer cust = customers.iterator().next();
		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForObjectWithRowMapper() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		params.add(new SqlParameterValue(Types.DECIMAL, 1));
		params.add("UK");

		Customer cust = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
				(rs, rownum) -> {
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				}).single();

		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForStreamWithRowMapper() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		params.add(new SqlParameterValue(Types.DECIMAL, 1));
		params.add("UK");
		AtomicInteger count = new AtomicInteger();

		try (Stream<Customer> s = client.sql(SELECT_INDEXED_PARAMETERS).params(params).query(
				(rs, rownum) -> {
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				}).stream()) {
			s.forEach(cust -> {
				count.incrementAndGet();
				assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
				assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
			});
		}

		assertThat(count.get()).isEqualTo(1);
		verify(connection).prepareStatement(SELECT_INDEXED_PARAMETERS);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void update() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		params.add(1);
		params.add(1);
		int rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).params(params).update();

		assertThat(rowsAffected).isEqualTo(1);
		verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
		verify(preparedStatement).setObject(1, 1);
		verify(preparedStatement).setObject(2, 1);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateSingleRow(boolean supportsBatchUpdates) throws SQLException {
		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(new int[] {1});
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(1);
		}

		int[] rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).batch()
				.param(1, 1).param(2, 1).add()
				.update();

		assertThat(rowsAffected).containsExactly(1);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
		inOrder.verify(preparedStatement).setObject(1, 1);
		inOrder.verify(preparedStatement).setObject(2, 1);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateMultipleRows(boolean supportsBatchUpdates) throws SQLException {
		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(new int[] {1, 1});
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(1);
		}

		int[] rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).batch()
				.param(1, 1).param(2, 1).add()
				.param(1, 2).param(2, 2).add()
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
		inOrder.verify(preparedStatement).setObject(1, 1);
		inOrder.verify(preparedStatement).setObject(2, 1);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 2);
		inOrder.verify(preparedStatement).setObject(2, 2);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@Test
	void updateWithTypedParameters() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		params.add(new SqlParameterValue(Types.DECIMAL, 1));
		params.add(new SqlParameterValue(Types.INTEGER, 1));
		int rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).params(params).update();

		assertThat(rowsAffected).isEqualTo(1);
		verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setObject(2, 1, Types.INTEGER);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithTypedParameters(boolean supportsBatchUpdates) throws SQLException {
		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(new int[] {1, 1});
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(1);
		}

		int[] rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).batch()
				.param(1, new SqlParameterValue(Types.DECIMAL, 1))
				.param(2, new SqlParameterValue(Types.INTEGER, 1))
				.add()
				.param(1, new SqlParameterValue(Types.DECIMAL, 2))
				.param(2, new SqlParameterValue(Types.INTEGER, 2))
				.add()
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
		inOrder.verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		inOrder.verify(preparedStatement).setObject(2, 1, Types.INTEGER);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 2, Types.DECIMAL);
		inOrder.verify(preparedStatement).setObject(2, 2, Types.INTEGER);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithParametersAndSqlType(boolean supportsBatchUpdates) throws SQLException {
		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(new int[] {1, 1});
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(1);
		}

		int[] rowsAffected = client.sql(UPDATE_INDEXED_PARAMETERS).batch()
				.param(1, 1, Types.DECIMAL)
				.param(2, 1, Types.INTEGER)
				.add()
				.param(1, 2, Types.DECIMAL)
				.param(2, 2, Types.INTEGER)
				.add()
				.update();

		assertThat(rowsAffected).containsExactly(1, 1);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement(UPDATE_INDEXED_PARAMETERS);
		inOrder.verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		inOrder.verify(preparedStatement).setObject(2, 1, Types.INTEGER);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 2, Types.DECIMAL);
		inOrder.verify(preparedStatement).setObject(2, 2, Types.INTEGER);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@Test
	void updateWithGeneratedKeys() throws SQLException {
		given(resultSetMetaData.getColumnCount()).willReturn(1);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("1");
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
		given(connection.prepareStatement(INSERT_GENERATE_KEYS, PreparedStatement.RETURN_GENERATED_KEYS))
				.willReturn(preparedStatement);

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int rowsAffected = client.sql(INSERT_GENERATE_KEYS).param("rod").update(generatedKeyHolder);

		assertThat(rowsAffected).isEqualTo(1);
		assertThat(generatedKeyHolder.getKeyList()).hasSize(1);
		assertThat(generatedKeyHolder.getKey()).isEqualTo(11);
		verify(preparedStatement).setString(1, "rod");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithGeneratedKeys(boolean supportsBatchUpdates) throws SQLException {
		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(new int[] {1, 1});
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(1);
		}
		given(resultSetMetaData.getColumnCount()).willReturn(1);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("id");
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getObject(1)).willReturn(11, 12);
		given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
		given(connection.prepareStatement(INSERT_GENERATE_KEYS, PreparedStatement.RETURN_GENERATED_KEYS))
				.willReturn(preparedStatement);

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int[] rowsAffected = client.sql(INSERT_GENERATE_KEYS).batch()
				.param("rod").add()
				.param("johnson").add()
				.update(generatedKeyHolder);

		assertThat(rowsAffected).containsExactly(1, 1);
		assertThat(generatedKeyHolder.getKeyList()).containsExactly(Map.of("id", 11), Map.of("id", 12));
		InOrder inOrder = inOrder(connection, preparedStatement, resultSet);
		inOrder.verify(connection).prepareStatement(INSERT_GENERATE_KEYS, PreparedStatement.RETURN_GENERATED_KEYS);
		inOrder.verify(preparedStatement).setString(1, "rod");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		else {
			inOrder.verify(resultSet).close();
		}
		inOrder.verify(preparedStatement).setString(1, "johnson");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(resultSet).close();
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@Test
	void updateWithGeneratedKeysAndKeyColumnNames() throws SQLException {
		given(resultSetMetaData.getColumnCount()).willReturn(1);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("1");
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
		given(connection.prepareStatement(INSERT_GENERATE_KEYS, new String[] {"id"}))
				.willReturn(preparedStatement);

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int rowsAffected = client.sql(INSERT_GENERATE_KEYS).param("rod").update(generatedKeyHolder, "id");

		assertThat(rowsAffected).isEqualTo(1);
		assertThat(generatedKeyHolder.getKeyList()).hasSize(1);
		assertThat(generatedKeyHolder.getKey()).isEqualTo(11);
		verify(preparedStatement).setString(1, "rod");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithGeneratedKeysAndKeyColumnNames(boolean supportsBatchUpdates) throws SQLException {
		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(new int[] {1, 1});
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(1);
		}
		given(resultSetMetaData.getColumnCount()).willReturn(1);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("id");
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getObject(1)).willReturn(11, 12);
		given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
		given(connection.prepareStatement(INSERT_GENERATE_KEYS, new String[] {"id"}))
				.willReturn(preparedStatement);

		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int[] rowsAffected = client.sql(INSERT_GENERATE_KEYS).batch()
				.param("rod").add()
				.param("johnson").add()
				.update(generatedKeyHolder, "id");

		assertThat(rowsAffected).containsExactly(1, 1);
		assertThat(generatedKeyHolder.getKeyList()).containsExactly(Map.of("id", 11), Map.of("id", 12));
		InOrder inOrder = inOrder(connection, preparedStatement, resultSet);
		inOrder.verify(connection).prepareStatement(INSERT_GENERATE_KEYS, new String[] {"id"});
		inOrder.verify(preparedStatement).setString(1, "rod");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		else {
			inOrder.verify(resultSet).close();
		}
		inOrder.verify(preparedStatement).setString(1, "johnson");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(resultSet).close();
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

}
