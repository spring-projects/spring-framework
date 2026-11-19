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

package org.springframework.jdbc.core.namedparam;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import org.springframework.jdbc.Customer;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Nikita Khateev
 * @author Fedor Bobin
 * @author Yanming Zhou
 */
class NamedParameterJdbcTemplateTests {

	private static final String SELECT_NAMED_PARAMETERS =
			"select id, forename from custmr where id = :id and country = :country";
	private static final String SELECT_NAMED_PARAMETERS_PARSED =
			"select id, forename from custmr where id = ? and country = ?";
	private static final String SELECT_NO_PARAMETERS =
			"select id, forename from custmr";

	private static final String INSERT_NAMED_PARAMETERS =
			"insert into custmr(forename,country) values (:forename,:country)";

	private static final String UPDATE_NAMED_PARAMETERS =
			"update seat_status set booking_id = null where performance_id = :perfId and price_band_id = :priceId";
	private static final String UPDATE_NAMED_PARAMETERS_PARSED =
			"update seat_status set booking_id = null where performance_id = ? and price_band_id = ?";

	private static final String UPDATE_ARRAY_PARAMETERS =
			"update customer set type = array[:typeIds] where id = :id";
	private static final String UPDATE_ARRAY_PARAMETERS_PARSED =
			"update customer set type = array[?, ?, ?] where id = ?";

	private static final String[] COLUMN_NAMES = new String[] {"id", "forename"};


	private final Connection connection = mock();

	private final DataSource dataSource = mock();

	private final PreparedStatement preparedStatement = mock();

	private final ResultSet resultSet = mock();

	private final DatabaseMetaData databaseMetaData = mock();

	private final NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(dataSource);

	private final Map<String, Object> params = new HashMap<>();


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
	void nullDataSourceProvidedToCtor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				new NamedParameterJdbcTemplate((DataSource) null));
	}

	@Test
	void nullJdbcTemplateProvidedToCtor() {
		assertThatIllegalArgumentException().isThrownBy(() ->
		new NamedParameterJdbcTemplate((JdbcOperations) null));
	}

	@Test
	void templateConfiguration() {
		assertThat(namedParameterTemplate.getJdbcTemplate().getDataSource()).isSameAs(dataSource);
	}

	@Test
	void execute() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		params.put("perfId", 1);
		params.put("priceId", 1);
		Object result = namedParameterTemplate.execute(UPDATE_NAMED_PARAMETERS, params,
				(PreparedStatementCallback<Object>) ps -> {
					assertThat(ps).isEqualTo(preparedStatement);
					ps.executeUpdate();
					return "result";
				});

		assertThat(result).isEqualTo("result");
		verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(preparedStatement).setObject(1, 1);
		verify(preparedStatement).setObject(2, 1);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Disabled("SPR-16340")
	@Test
	void executeArray() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		List<Integer> typeIds = Arrays.asList(1, 2, 3);

		params.put("typeIds", typeIds);
		params.put("id", 1);
		Object result = namedParameterTemplate.execute(UPDATE_ARRAY_PARAMETERS, params,
				(PreparedStatementCallback<Object>) ps -> {
					assertThat(ps).isEqualTo(preparedStatement);
					ps.executeUpdate();
					return "result";
				});

		assertThat(result).isEqualTo("result");
		verify(connection).prepareStatement(UPDATE_ARRAY_PARAMETERS_PARSED);
		verify(preparedStatement).setObject(1, 1);
		verify(preparedStatement).setObject(2, 2);
		verify(preparedStatement).setObject(3, 3);
		verify(preparedStatement).setObject(4, 1);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void executeWithTypedParameters() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		params.put("perfId", new SqlParameterValue(Types.DECIMAL, 1));
		params.put("priceId", new SqlParameterValue(Types.INTEGER, 1));
		Object result = namedParameterTemplate.execute(UPDATE_NAMED_PARAMETERS, params,
				(PreparedStatementCallback<Object>) ps -> {
					assertThat(ps).isEqualTo(preparedStatement);
					ps.executeUpdate();
					return "result";
				});

		assertThat(result).isEqualTo("result");
		verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setObject(2, 1, Types.INTEGER);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void executeNoParameters() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		Object result = namedParameterTemplate.execute(SELECT_NO_PARAMETERS,
				(PreparedStatementCallback<Object>) ps -> {
					assertThat(ps).isEqualTo(preparedStatement);
					ps.executeQuery();
					return "result";
				});

		assertThat(result).isEqualTo("result");
		verify(connection).prepareStatement(SELECT_NO_PARAMETERS);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryWithResultSetExtractor() throws SQLException {
		given(resultSet.next()).willReturn(true);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		params.put("country", "UK");
		Customer cust = namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, params,
				rs -> {
					rs.next();
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				});

		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
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

		Customer cust = namedParameterTemplate.query(SELECT_NO_PARAMETERS,
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

		params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		params.put("country", "UK");
		final List<Customer> customers = new ArrayList<>();
		namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, params, rs -> {
			Customer cust = new Customer();
			cust.setId(rs.getInt(COLUMN_NAMES[0]));
			cust.setForename(rs.getString(COLUMN_NAMES[1]));
			customers.add(cust);
		});

		assertThat(customers).hasSize(1);
		assertThat(customers.get(0).getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(customers.get(0).getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
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
		namedParameterTemplate.query(SELECT_NO_PARAMETERS, rs -> {
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

		params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		params.put("country", "UK");
		List<Customer> customers = namedParameterTemplate.query(SELECT_NAMED_PARAMETERS, params,
				(rs, rownum) -> {
					Customer cust = new Customer();
					cust.setId(rs.getInt(COLUMN_NAMES[0]));
					cust.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust;
				});

		assertThat(customers).hasSize(1);
		assertThat(customers.get(0).getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(customers.get(0).getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
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

		List<Customer> customers = namedParameterTemplate.query(SELECT_NO_PARAMETERS,
				(rs, rownum) -> {
					Customer cust = new Customer();
					cust.setId(rs.getInt(COLUMN_NAMES[0]));
					cust.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust;
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
	void queryForObjectWithRowMapper() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		params.put("country", "UK");

		Customer cust = namedParameterTemplate.queryForObject(SELECT_NAMED_PARAMETERS, params,
				(rs, rownum) -> {
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				});

		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
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

		params.put("id", new SqlParameterValue(Types.DECIMAL, 1));
		params.put("country", "UK");
		AtomicInteger count = new AtomicInteger();

		try (Stream<Customer> s = namedParameterTemplate.queryForStream(SELECT_NAMED_PARAMETERS, params,
				(rs, rownum) -> {
					Customer cust1 = new Customer();
					cust1.setId(rs.getInt(COLUMN_NAMES[0]));
					cust1.setForename(rs.getString(COLUMN_NAMES[1]));
					return cust1;
				})) {
			s.forEach(cust -> {
				count.incrementAndGet();
				assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
				assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
			});
		}

		assertThat(count.get()).isEqualTo(1);
		verify(connection).prepareStatement(SELECT_NAMED_PARAMETERS_PARSED);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void update() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		params.put("perfId", 1);
		params.put("priceId", 1);
		int rowsAffected = namedParameterTemplate.update(UPDATE_NAMED_PARAMETERS, params);

		assertThat(rowsAffected).isEqualTo(1);
		verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(preparedStatement).setObject(1, 1);
		verify(preparedStatement).setObject(2, 1);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void updateWithTypedParameters() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);

		params.put("perfId", new SqlParameterValue(Types.DECIMAL, 1));
		params.put("priceId", new SqlParameterValue(Types.INTEGER, 1));
		int rowsAffected = namedParameterTemplate.update(UPDATE_NAMED_PARAMETERS, params);

		assertThat(rowsAffected).isEqualTo(1);
		verify(connection).prepareStatement(UPDATE_NAMED_PARAMETERS_PARSED);
		verify(preparedStatement).setObject(1, 1, Types.DECIMAL);
		verify(preparedStatement).setObject(2, 1, Types.INTEGER);
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithPlainMap(boolean supportsBatchUpdates) throws SQLException {
		@SuppressWarnings("unchecked")
		final Map<String, Integer>[] ids = new Map[2];
		ids[0] = Collections.singletonMap("id", 100);
		ids[1] = Collections.singletonMap("id", 200);
		final int[] rowsAffected = new int[] {1, 2};

		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(rowsAffected);
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1]);
		}
		NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

		int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertThat(actualRowsAffected).as("executed 2 updates").isEqualTo(rowsAffected);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
		inOrder.verify(preparedStatement).setObject(1, 100);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 200);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@Test
	void batchUpdateWithEmptyMap() {
		@SuppressWarnings("unchecked")
		final Map<String, Integer>[] ids = new Map[0];
		NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

		int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertThat(actualRowsAffected.length).as("executed 0 updates").isEqualTo(0);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithSqlParameterSource(boolean supportsBatchUpdates) throws SQLException {
		SqlParameterSource[] ids = new SqlParameterSource[2];
		ids[0] = new MapSqlParameterSource("id", 100);
		ids[1] = new MapSqlParameterSource("id", 200);
		final int[] rowsAffected = new int[] {1, 2};

		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(rowsAffected);
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1]);
		}
		NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

		int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertThat(actualRowsAffected).as("executed 2 updates").isEqualTo(rowsAffected);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
		inOrder.verify(preparedStatement).setObject(1, 100);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 200);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithInClause(boolean supportsBatchUpdates) throws SQLException {
		@SuppressWarnings("unchecked")
		Map<String, Object>[] parameters = new Map[3];
		parameters[0] = Collections.singletonMap("ids", Arrays.asList(1, 2));
		parameters[1] = Collections.singletonMap("ids", Arrays.asList("3", "4"));
		parameters[2] = Collections.singletonMap("ids", (Iterable<Integer>) () -> Arrays.asList(5, 6).iterator());

		final int[] rowsAffected = new int[] {1, 2, 3};
		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(rowsAffected);
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1], rowsAffected[2]);
		}

		JdbcTemplate template = new JdbcTemplate(dataSource, false);
		NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(template);

		int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
				"delete sometable where id in (:ids)",
				parameters
		);

		assertThat(actualRowsAffected).as("executed 3 updates").isEqualTo(rowsAffected);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement("delete sometable where id in (?, ?)");
		inOrder.verify(preparedStatement).setObject(1, 1);
		inOrder.verify(preparedStatement).setObject(2, 2);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setString(1, "3");
		inOrder.verify(preparedStatement).setString(2, "4");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 5);
		inOrder.verify(preparedStatement).setObject(2, 6);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithSqlParameterSourcePlusTypeInfo(boolean supportsBatchUpdates) throws SQLException {
		SqlParameterSource[] ids = new SqlParameterSource[3];
		ids[0] = new MapSqlParameterSource().addValue("id", null, Types.NULL);
		ids[1] = new MapSqlParameterSource().addValue("id", 100, Types.NUMERIC);
		ids[2] = new MapSqlParameterSource().addValue("id", 200, Types.NUMERIC);
		final int[] rowsAffected = new int[] {1, 2, 3};

		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(rowsAffected);
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1], rowsAffected[2]);
		}
		NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

		int[] actualRowsAffected = namedParameterTemplate.batchUpdate(
				"UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = :id", ids);
		assertThat(actualRowsAffected).as("executed 3 updates").isEqualTo(rowsAffected);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement("UPDATE NOSUCHTABLE SET DATE_DISPATCHED = SYSDATE WHERE ID = ?");
		inOrder.verify(preparedStatement).setNull(1, Types.NULL);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 100, Types.NUMERIC);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setObject(1, 200, Types.NUMERIC);
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithGeneratedKeys(boolean supportsBatchUpdates) throws SQLException {
		final SqlParameterSource[] batchArgs = new SqlParameterSource[2];
		batchArgs[0] = new MapSqlParameterSource(Map.of("forename", "foo", "country", "UK"));
		batchArgs[1] = new MapSqlParameterSource(Map.of("forename", "bar", "country", "US"));
		final int[] rowsAffected = new int[] {1, 1};

		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(rowsAffected);
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1]);
		}
		given(connection.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).willReturn(preparedStatement);
		NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

		int[] actualRowsAffected = namedParameterTemplate.batchUpdate(INSERT_NAMED_PARAMETERS, batchArgs, new GeneratedKeyHolder());
		assertThat(actualRowsAffected).as("executed 2 updates").isEqualTo(rowsAffected);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement("insert into custmr(forename,country) values (?,?)", Statement.RETURN_GENERATED_KEYS);
		inOrder.verify(preparedStatement).setString(1, "foo");
		inOrder.verify(preparedStatement).setString(2, "UK");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setString(1, "bar");
		inOrder.verify(preparedStatement).setString(2, "US");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void batchUpdateWithGeneratedKeysAndKeyColumnNames(boolean supportsBatchUpdates) throws SQLException {
		final SqlParameterSource[] batchArgs = new SqlParameterSource[2];
		batchArgs[0] = new MapSqlParameterSource(Map.of("forename", "foo", "country", "UK"));
		batchArgs[1] = new MapSqlParameterSource(Map.of("forename", "bar", "country", "US"));
		final int[] rowsAffected = new int[] {1, 1};
		final String[] keyColumnNames = new String[] {"id"};

		if (supportsBatchUpdates) {
			given(connection.getMetaData()).willReturn(databaseMetaData);
			given(preparedStatement.executeBatch()).willReturn(rowsAffected);
		}
		else {
			given(preparedStatement.executeUpdate()).willReturn(rowsAffected[0], rowsAffected[1]);
		}
		given(connection.prepareStatement(anyString(), eq(keyColumnNames))).willReturn(preparedStatement);
		NamedParameterJdbcTemplate namedParameterTemplate = new NamedParameterJdbcTemplate(new JdbcTemplate(dataSource, false));

		int[] actualRowsAffected = namedParameterTemplate.batchUpdate(INSERT_NAMED_PARAMETERS, batchArgs, new GeneratedKeyHolder(), keyColumnNames);
		assertThat(actualRowsAffected).as("executed 2 updates").isEqualTo(rowsAffected);
		InOrder inOrder = inOrder(connection, preparedStatement);
		inOrder.verify(connection).prepareStatement("insert into custmr(forename,country) values (?,?)", keyColumnNames);
		inOrder.verify(preparedStatement).setString(1, "foo");
		inOrder.verify(preparedStatement).setString(2, "UK");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).setString(1, "bar");
		inOrder.verify(preparedStatement).setString(2, "US");
		if (supportsBatchUpdates) {
			inOrder.verify(preparedStatement).addBatch();
		}
		inOrder.verify(preparedStatement).close();
		inOrder.verify(connection).close();
	}

}
