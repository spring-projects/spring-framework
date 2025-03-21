/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.jdbc.object;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.Customer;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Trevor Cook
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Yanming Zhou
 */
class SqlQueryTests {

	private static final String SELECT_ID =
			"select id from custmr";
	private static final String SELECT_ID_WHERE =
			"select id from custmr where forename = ? and id = ?";
	private static final String SELECT_FORENAME =
			"select forename from custmr";
	private static final String SELECT_FORENAME_EMPTY =
			"select forename from custmr WHERE 1 = 2";
	private static final String SELECT_ID_FORENAME_WHERE =
			"select id, forename from prefix:custmr where forename = ?";
	private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS =
			"select id, forename from custmr where id = :id and country = :country";
	private static final String SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED =
			"select id, forename from custmr where id = ? and country = ?";
	private static final String SELECT_ID_FORENAME_WHERE_ID_IN_LIST_1 =
			"select id, forename from custmr where id in (?, ?)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_IN_LIST_2 =
			"select id, forename from custmr where id in (:ids)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_REUSED_1 =
			"select id, forename from custmr where id = ? or id = ?)";
	private static final String SELECT_ID_FORENAME_WHERE_ID_REUSED_2 =
			"select id, forename from custmr where id = :id1 or id = :id1)";
	private static final String SELECT_ID_FORENAME_WHERE_ID =
			"select id, forename from custmr where id <= ?";

	private static final String[] COLUMN_NAMES = new String[] {"id", "forename"};
	private static final int[] COLUMN_TYPES = new int[] {Types.INTEGER, Types.VARCHAR};


	private Connection connection = mock();

	private DataSource dataSource = mock();

	private PreparedStatement preparedStatement = mock();

	private ResultSet resultSet = mock();


	@BeforeEach
	void setUp() throws Exception {
		given(this.dataSource.getConnection()).willReturn(this.connection);
		given(this.connection.prepareStatement(anyString())).willReturn(this.preparedStatement);
		given(preparedStatement.executeQuery()).willReturn(resultSet);
	}

	@Test
	void testQueryWithoutParams() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(1);

		SqlQuery<Integer> query = new MappingSqlQueryWithParameters<>() {
			@Override
			protected Integer mapRow(ResultSet rs, int rownum, Object @Nullable [] params, @Nullable Map<? ,?> context)
					throws SQLException {
				assertThat(params).as("params were null").isNull();
				assertThat(context).as("context was null").isNull();
				return rs.getInt(1);
			}
		};
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID);
		query.compile();
		List<Integer> list = query.execute();

		assertThat(list).containsExactly(1);
		verify(connection).prepareStatement(SELECT_ID);
		verify(resultSet).close();
		verify(preparedStatement).close();
	}

	@Test
	void testStreamWithoutParams() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(1);

		SqlQuery<Integer> query = new MappingSqlQueryWithParameters<>() {
			@Override
			protected Integer mapRow(ResultSet rs, int rownum, Object @Nullable [] params, @Nullable Map<? ,?> context)
					throws SQLException {
				assertThat(params).as("params were null").isNull();
				assertThat(context).as("context was null").isNull();
				return rs.getInt(1);
			}
		};
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID);
		query.compile();
		try (Stream<Integer> stream = query.stream()) {
			List<Integer> list = stream.toList();
			assertThat(list).containsExactly(1);
		}
		verify(connection).prepareStatement(SELECT_ID);
		verify(resultSet).close();
		verify(preparedStatement).close();
	}

	@Test
	void testStreamByNamedParam() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");
		given(connection.prepareStatement(SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
		).willReturn(preparedStatement);

		SqlQuery<Customer> query = new MappingSqlQueryWithParameters<>() {
			@Override
			protected Customer mapRow(ResultSet rs, int rownum, Object @Nullable [] params, @Nullable Map<? ,?> context)
					throws SQLException {
				assertThat(params).as("params were not null").isNotNull();
				assertThat(context).as("context was null").isNull();
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}
		};
		query.declareParameter(new SqlParameter("id", Types.NUMERIC));
		query.declareParameter(new SqlParameter("country", Types.VARCHAR));
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID_FORENAME_NAMED_PARAMETERS);
		query.compile();
		try (Stream<Customer> stream = query.streamByNamedParam(Map.of("id", 1, "country", "UK"))) {
			List<Customer> list = stream.toList();
			assertThat(list).hasSize(1);
			Customer customer = list.get(0);
			assertThat(customer.getId()).isEqualTo(1);
			assertThat(customer.getForename()).isEqualTo("rod");
		}
		verify(connection).prepareStatement(SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED);
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
	}

	@Test
	void testQueryWithoutEnoughParams() {
		MappingSqlQuery<Integer> query = new MappingSqlQuery<>() {
			@Override
			protected Integer mapRow(ResultSet rs, int rownum) throws SQLException {
				return rs.getInt(1);
			}
		};
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID_WHERE);
		query.declareParameter(new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
		query.declareParameter(new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
		query.compile();

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(
				query::execute);
	}

	@Test
	void testQueryWithMissingMapParams() {
		MappingSqlQuery<Integer> query = new MappingSqlQuery<>() {
			@Override
			protected Integer mapRow(ResultSet rs, int rownum) throws SQLException {
				return rs.getInt(1);
			}
		};
		query.setDataSource(dataSource);
		query.setSql(SELECT_ID_WHERE);
		query.declareParameter(new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
		query.declareParameter(new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
		query.compile();

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				query.executeByNamedParam(Collections.singletonMap(COLUMN_NAMES[0], "value")));
	}

	@Test
	void testStringQueryWithResults() throws Exception {
		String[] dbResults = new String[] { "alpha", "beta", "charlie" };
		given(resultSet.next()).willReturn(true, true, true, false);
		given(resultSet.getString(1)).willReturn(dbResults[0], dbResults[1], dbResults[2]);
		StringQuery query = new StringQuery(dataSource, SELECT_FORENAME);
		String[] results = query.run();
		assertThat(results).isEqualTo(dbResults);
		verify(connection).prepareStatement(SELECT_FORENAME);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testStringQueryWithoutResults() throws SQLException {
		given(resultSet.next()).willReturn(false);
		StringQuery query = new StringQuery(dataSource, SELECT_FORENAME_EMPTY);
		String[] results = query.run();
		assertThat(results).isEqualTo(new String[0]);
		verify(connection).prepareStatement(SELECT_FORENAME_EMPTY);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testFindCustomerIntInt() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, int otherNum) {
				return findObject(id, otherNum);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer(1, 1);

		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
		verify(connection).prepareStatement(SELECT_ID_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testFindCustomerString() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(String id) {
				return findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer("rod");

		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(preparedStatement).setString(1, "rod");
		verify(connection).prepareStatement(SELECT_ID_FORENAME_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testFindCustomerMixed() throws SQLException {
		reset(connection);
		PreparedStatement preparedStatement2 = mock();
		ResultSet resultSet2 = mock();
		given(preparedStatement2.executeQuery()).willReturn(resultSet2);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");
		given(resultSet2.next()).willReturn(false);
		given(connection.prepareStatement(SELECT_ID_WHERE)).willReturn(preparedStatement, preparedStatement2);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(COLUMN_NAMES[0], COLUMN_TYPES[0]));
				declareParameter(new SqlParameter(COLUMN_NAMES[1], COLUMN_TYPES[1]));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, String name) {
				return findObject(id, name);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);

		Customer cust1 = query.findCustomer(1, "rod");
		assertThat(cust1).as("Found customer").isNotNull();
		assertThat(cust1.getId()).as("Customer id was assigned correctly").isEqualTo(1);

		Customer cust2 = query.findCustomer(1, "Roger");
		assertThat(cust2).as("No customer found").isNull();

		verify(preparedStatement).setObject(1, 1, Types.INTEGER);
		verify(preparedStatement).setString(2, "rod");
		verify(preparedStatement2).setObject(1, 1, Types.INTEGER);
		verify(preparedStatement2).setString(2, "Roger");
		verify(resultSet).close();
		verify(resultSet2).close();
		verify(preparedStatement).close();
		verify(preparedStatement2).close();
		verify(connection, times(2)).close();
	}

	@Test
	void testFindTooManyCustomers() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "rod");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(String id) {
				return findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() ->
				query.findCustomer("rod"));
		verify(preparedStatement).setString(1, "rod");
		verify(connection).prepareStatement(SELECT_ID_FORENAME_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testListCustomersIntInt() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "dave");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_WHERE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Customer> list = query.execute(1, 1);
		assertThat(list.size()).as("2 results in list").isEqualTo(2);
		assertThat(list.get(0).getForename()).isEqualTo("rod");
		assertThat(list.get(1).getForename()).isEqualTo("dave");
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
		verify(connection).prepareStatement(SELECT_ID_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testListCustomersString() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "dave");

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				declareParameter(new SqlParameter(Types.VARCHAR));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Customer> list = query.execute("one");
		assertThat(list.size()).as("2 results in list").isEqualTo(2);
		assertThat(list.get(0).getForename()).isEqualTo("rod");
		assertThat(list.get(1).getForename()).isEqualTo("dave");
		verify(preparedStatement).setString(1, "one");
		verify(connection).prepareStatement(SELECT_ID_FORENAME_WHERE);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testFancyCustomerQuery() throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");

		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
			).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id) {
				return findObject(id);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer(1);
		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testUnnamedParameterDeclarationWithNamedParameterQuery() {
		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id) {
				Map<String, Integer> params = new HashMap<>();
				params.put("id", id);
				return executeByNamedParam(params).get(0);
			}
		}

		// Query should not succeed since parameter declaration did not specify parameter name
		CustomerQuery query = new CustomerQuery(dataSource);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				query.findCustomer(1));
	}

	@Test
	void testNamedParameterCustomerQueryWithUnnamedDeclarations()
			throws SQLException {
		doTestNamedParameterCustomerQuery(false);
	}

	@Test
	void testNamedParameterCustomerQueryWithNamedDeclarations()
			throws SQLException {
		doTestNamedParameterCustomerQuery(true);
	}

	private void doTestNamedParameterCustomerQuery(final boolean namedDeclarations)
			throws SQLException {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt("id")).willReturn(1);
		given(resultSet.getString("forename")).willReturn("rod");
		given(connection.prepareStatement(SELECT_ID_FORENAME_NAMED_PARAMETERS_PARSED,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
			).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_NAMED_PARAMETERS);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				if (namedDeclarations) {
					declareParameter(new SqlParameter("country", Types.VARCHAR));
					declareParameter(new SqlParameter("id", Types.NUMERIC));
				}
				else {
					declareParameter(new SqlParameter(Types.NUMERIC));
					declareParameter(new SqlParameter(Types.VARCHAR));
				}
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public Customer findCustomer(int id, String country) {
				Map<String, Object> params = new HashMap<>();
				params.put("id", id);
				params.put("country", country);
				return executeByNamedParam(params).get(0);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		Customer cust = query.findCustomer(1, "UK");
		assertThat(cust.getId()).as("Customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.getForename()).as("Customer forename was assigned correctly").isEqualTo("rod");
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setString(2, "UK");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testNamedParameterInListQuery() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "juergen");

		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID_IN_LIST_1,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)
			).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_IN_LIST_2);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("ids", Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List<Customer> findCustomers(List<Integer> ids) {
				Map<String, Object> params = new HashMap<>();
				params.put("ids", ids);
				return executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Integer> ids = new ArrayList<>();
		ids.add(1);
		ids.add(2);
		List<Customer> cust = query.findCustomers(ids);

		assertThat(cust.size()).as("We got two customers back").isEqualTo(2);
		assertThat(cust.get(0).getId()).as("First customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.get(0).getForename()).as("First customer forename was assigned correctly").isEqualTo("rod");
		assertThat(cust.get(1).getId()).as("Second customer id was assigned correctly").isEqualTo(2);
		assertThat(cust.get(1).getForename()).as("Second customer forename was assigned correctly")
				.isEqualTo("juergen");
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 2, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testNamedParameterQueryReusingParameter() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(resultSet.getString("forename")).willReturn("rod", "juergen");

		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID_REUSED_1,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)).willReturn(preparedStatement)
;

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_REUSED_2);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("id1", Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List<Customer> findCustomers(Integer id) {
				Map<String, Object> params = new HashMap<>();
				params.put("id1", id);
				return executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		List<Customer> cust = query.findCustomers(1);

		assertThat(cust.size()).as("We got two customers back").isEqualTo(2);
		assertThat(cust.get(0).getId()).as("First customer id was assigned correctly").isEqualTo(1);
		assertThat(cust.get(0).getForename()).as("First customer forename was assigned correctly").isEqualTo("rod");
		assertThat(cust.get(1).getId()).as("Second customer id was assigned correctly").isEqualTo(2);
		assertThat(cust.get(1).getForename()).as("Second customer forename was assigned correctly")
				.isEqualTo("juergen");

		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void testNamedParameterUsingInvalidQuestionMarkPlaceHolders()
			throws SQLException {
		given(
		connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID_REUSED_1,
				ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)).willReturn(preparedStatement);

		class CustomerQuery extends MappingSqlQuery<Customer> {

			public CustomerQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID_REUSED_1);
				setResultSetType(ResultSet.TYPE_SCROLL_SENSITIVE);
				declareParameter(new SqlParameter("id1", Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer mapRow(ResultSet rs, int rownum) throws SQLException {
				Customer cust = new Customer();
				cust.setId(rs.getInt(COLUMN_NAMES[0]));
				cust.setForename(rs.getString(COLUMN_NAMES[1]));
				return cust;
			}

			public List<Customer> findCustomers(Integer id1) {
				Map<String, Integer> params = new HashMap<>();
				params.put("id1", id1);
				return executeByNamedParam(params);
			}
		}

		CustomerQuery query = new CustomerQuery(dataSource);
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				query.findCustomers(1));
	}

	@Test
	void testUpdateCustomers() throws SQLException {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getInt("id")).willReturn(1, 2);
		given(connection.prepareStatement(SELECT_ID_FORENAME_WHERE_ID,
				ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)
			).willReturn(preparedStatement);

		class CustomerUpdateQuery extends UpdatableSqlQuery<Customer> {

			public CustomerUpdateQuery(DataSource ds) {
				super(ds, SELECT_ID_FORENAME_WHERE_ID);
				declareParameter(new SqlParameter(Types.NUMERIC));
				compile();
			}

			@Override
			protected Customer updateRow(ResultSet rs, int rownum, @Nullable Map<? ,?> context)
					throws SQLException {
				rs.updateString(2, "" + context.get(rs.getInt(COLUMN_NAMES[0])));
				return null;
			}
		}

		CustomerUpdateQuery query = new CustomerUpdateQuery(dataSource);
		Map<Integer, String> values = new HashMap<>(2);
		values.put(1, "Rod");
		values.put(2, "Thomas");
		query.execute(2, values);
		verify(resultSet).updateString(2, "Rod");
		verify(resultSet).updateString(2, "Thomas");
		verify(resultSet, times(2)).updateRow();
		verify(preparedStatement).setObject(1, 2, Types.NUMERIC);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	private static class StringQuery extends MappingSqlQuery<String> {

		public StringQuery(DataSource ds, String sql) {
			super(ds, sql);
			compile();
		}

		@Override
		protected String mapRow(ResultSet rs, int rownum) throws SQLException {
			return rs.getString(1);
		}

		public String[] run() {
			return StringUtils.toStringArray(execute());
		}
	}

}
