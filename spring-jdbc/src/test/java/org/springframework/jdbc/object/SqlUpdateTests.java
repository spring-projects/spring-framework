/*
 * Copyright 2002-2016 the original author or authors.
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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Trevor Cook
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlUpdateTests {

	private static final String UPDATE =
			"update seat_status set booking_id = null";

	private static final String UPDATE_INT =
			"update seat_status set booking_id = null where performance_id = ?";

	private static final String UPDATE_INT_INT =
			"update seat_status set booking_id = null where performance_id = ? and price_band_id = ?";

	private static final String UPDATE_NAMED_PARAMETERS =
			"update seat_status set booking_id = null where performance_id = :perfId and price_band_id = :priceId";

	private static final String UPDATE_STRING =
			"update seat_status set booking_id = null where name = ?";

	private static final String UPDATE_OBJECTS =
			"update seat_status set booking_id = null where performance_id = ? and price_band_id = ? and name = ? and confirmed = ?";

	private static final String INSERT_GENERATE_KEYS =
			"insert into show (name) values(?)";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private DataSource dataSource;

	private Connection connection;

	private PreparedStatement preparedStatement;

	private ResultSet resultSet;

	private ResultSetMetaData resultSetMetaData;


	@Before
	public void setUp() throws Exception {
		dataSource = mock(DataSource.class);
		connection = mock(Connection.class);
		preparedStatement = mock(PreparedStatement.class);
		resultSet = mock(ResultSet.class);
		resultSetMetaData = mock(ResultSetMetaData.class);
		given(dataSource.getConnection()).willReturn(connection);
	}

	@After
	public void verifyClosed() throws Exception {
		verify(preparedStatement).close();
		verify(connection).close();
	}


	@Test
	public void testUpdate() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

		Updater pc = new Updater();
		int rowsAffected = pc.run();

		assertEquals(1, rowsAffected);
	}

	@Test
	public void testUpdateInt() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(connection.prepareStatement(UPDATE_INT)).willReturn(preparedStatement);

		IntUpdater pc = new IntUpdater();
		int rowsAffected = pc.run(1);

		assertEquals(1, rowsAffected);
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
	}

	@Test
	public void testUpdateIntInt() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(connection.prepareStatement(UPDATE_INT_INT)).willReturn(preparedStatement);

		IntIntUpdater pc = new IntIntUpdater();
		int rowsAffected = pc.run(1, 1);

		assertEquals(1, rowsAffected);
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
	}

	@Test
	public void testNamedParameterUpdateWithUnnamedDeclarations() throws SQLException {
		doTestNamedParameterUpdate(false);
	}

	@Test
	public void testNamedParameterUpdateWithNamedDeclarations() throws SQLException {
		doTestNamedParameterUpdate(true);
	}

	private void doTestNamedParameterUpdate(final boolean namedDeclarations)
			throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(connection.prepareStatement(UPDATE_INT_INT)).willReturn(preparedStatement);

		class NamedParameterUpdater extends SqlUpdate {
			public NamedParameterUpdater() {
				setSql(UPDATE_NAMED_PARAMETERS);
				setDataSource(dataSource);
				if (namedDeclarations) {
					declareParameter(new SqlParameter("priceId", Types.DECIMAL));
					declareParameter(new SqlParameter("perfId", Types.NUMERIC));
				}
				else {
					declareParameter(new SqlParameter(Types.NUMERIC));
					declareParameter(new SqlParameter(Types.DECIMAL));
				}
				compile();
			}

			public int run(int performanceId, int type) {
				Map<String, Integer> params = new HashMap<String, Integer>();
				params.put("perfId", performanceId);
				params.put("priceId", type);
				return updateByNamedParam(params);
			}
		}

		NamedParameterUpdater pc = new NamedParameterUpdater();
		int rowsAffected = pc.run(1, 1);
		assertEquals(1, rowsAffected);
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.DECIMAL);
	}

	@Test
	public void testUpdateString() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(connection.prepareStatement(UPDATE_STRING)).willReturn(preparedStatement);

		StringUpdater pc = new StringUpdater();
		int rowsAffected = pc.run("rod");

		assertEquals(1, rowsAffected);
		verify(preparedStatement).setString(1, "rod");
	}

	@Test
	public void testUpdateMixed() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(connection.prepareStatement(UPDATE_OBJECTS)).willReturn(preparedStatement);

		MixedUpdater pc = new MixedUpdater();
		int rowsAffected = pc.run(1, 1, "rod", true);

		assertEquals(1, rowsAffected);
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC, 2);
		verify(preparedStatement).setString(3, "rod");
		verify(preparedStatement).setBoolean(4, Boolean.TRUE);
	}

	@Test
	public void testUpdateAndGeneratedKeys() throws SQLException {
		given(resultSetMetaData.getColumnCount()).willReturn(1);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("1");
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(preparedStatement.getGeneratedKeys()).willReturn(resultSet);
		given(connection.prepareStatement(INSERT_GENERATE_KEYS,
				PreparedStatement.RETURN_GENERATED_KEYS)
			).willReturn(preparedStatement);

		GeneratedKeysUpdater pc = new GeneratedKeysUpdater();
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int rowsAffected = pc.run("rod", generatedKeyHolder);

		assertEquals(1, rowsAffected);
		assertEquals(1, generatedKeyHolder.getKeyList().size());
		assertEquals(11, generatedKeyHolder.getKey().intValue());
		verify(preparedStatement).setString(1, "rod");
		verify(resultSet).close();
	}

	@Test
	public void testUpdateConstructor() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(1);
		given(connection.prepareStatement(UPDATE_OBJECTS)).willReturn(preparedStatement);
		ConstructorUpdater pc = new ConstructorUpdater();

		int rowsAffected = pc.run(1, 1, "rod", true);

		assertEquals(1, rowsAffected);
		verify(preparedStatement).setObject(1, 1, Types.NUMERIC);
		verify(preparedStatement).setObject(2, 1, Types.NUMERIC);
		verify(preparedStatement).setString(3, "rod");
		verify(preparedStatement).setBoolean(4, Boolean.TRUE);
	}

	@Test
	public void testUnderMaxRows() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(3);
		given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

		MaxRowsUpdater pc = new MaxRowsUpdater();

		int rowsAffected = pc.run();
		assertEquals(3, rowsAffected);
	}

	@Test
	public void testMaxRows() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(5);
		given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

		MaxRowsUpdater pc = new MaxRowsUpdater();
		int rowsAffected = pc.run();

		assertEquals(5, rowsAffected);
	}

	@Test
	public void testOverMaxRows() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(8);
		given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

		MaxRowsUpdater pc = new MaxRowsUpdater();

		thrown.expect(JdbcUpdateAffectedIncorrectNumberOfRowsException.class);
		pc.run();
	}

	@Test
	public void testRequiredRows() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(3);
		given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);

		RequiredRowsUpdater pc = new RequiredRowsUpdater();
		int rowsAffected = pc.run();

		assertEquals(3, rowsAffected);
	}

	@Test
	public void testNotRequiredRows() throws SQLException {
		given(preparedStatement.executeUpdate()).willReturn(2);
		given(connection.prepareStatement(UPDATE)).willReturn(preparedStatement);
		thrown.expect(JdbcUpdateAffectedIncorrectNumberOfRowsException.class);
		RequiredRowsUpdater pc = new RequiredRowsUpdater();
		pc.run();
	}

	private class Updater extends SqlUpdate {

		public Updater() {
			setSql(UPDATE);
			setDataSource(dataSource);
			compile();
		}

		public int run() {
			return update();
		}
	}


	private class IntUpdater extends SqlUpdate {

		public IntUpdater() {
			setSql(UPDATE_INT);
			setDataSource(dataSource);
			declareParameter(new SqlParameter(Types.NUMERIC));
			compile();
		}

		public int run(int performanceId) {
			return update(performanceId);
		}
	}


	private class IntIntUpdater extends SqlUpdate {

		public IntIntUpdater() {
			setSql(UPDATE_INT_INT);
			setDataSource(dataSource);
			declareParameter(new SqlParameter(Types.NUMERIC));
			declareParameter(new SqlParameter(Types.NUMERIC));
			compile();
		}

		public int run(int performanceId, int type) {
			return update(performanceId, type);
		}
	}


	private class StringUpdater extends SqlUpdate {

		public StringUpdater() {
			setSql(UPDATE_STRING);
			setDataSource(dataSource);
			declareParameter(new SqlParameter(Types.VARCHAR));
			compile();
		}

		public int run(String name) {
			return update(name);
		}
	}


	private class MixedUpdater extends SqlUpdate {

		public MixedUpdater() {
			setSql(UPDATE_OBJECTS);
			setDataSource(dataSource);
			declareParameter(new SqlParameter(Types.NUMERIC));
			declareParameter(new SqlParameter(Types.NUMERIC, 2));
			declareParameter(new SqlParameter(Types.VARCHAR));
			declareParameter(new SqlParameter(Types.BOOLEAN));
			compile();
		}

		public int run(int performanceId, int type, String name, boolean confirmed) {
			return update(performanceId, type, name, confirmed);
		}
	}


	private class GeneratedKeysUpdater extends SqlUpdate {

		public GeneratedKeysUpdater() {
			setSql(INSERT_GENERATE_KEYS);
			setDataSource(dataSource);
			declareParameter(new SqlParameter(Types.VARCHAR));
			setReturnGeneratedKeys(true);
			compile();
		}

		public int run(String name, KeyHolder generatedKeyHolder) {
			return update(new Object[] {name}, generatedKeyHolder);
		}
	}


	private class ConstructorUpdater extends SqlUpdate {

		public ConstructorUpdater() {
			super(dataSource, UPDATE_OBJECTS,
					new int[] {Types.NUMERIC, Types.NUMERIC, Types.VARCHAR, Types.BOOLEAN });
			compile();
		}

		public int run(int performanceId, int type, String name, boolean confirmed) {
			return update(performanceId, type, name, confirmed);
		}
	}


	private class MaxRowsUpdater extends SqlUpdate {

		public MaxRowsUpdater() {
			setSql(UPDATE);
			setDataSource(dataSource);
			setMaxRowsAffected(5);
			compile();
		}

		public int run() {
			return update();
		}
	}


	private class RequiredRowsUpdater extends SqlUpdate {

		public RequiredRowsUpdater() {
			setSql(UPDATE);
			setDataSource(dataSource);
			setRequiredRowsAffected(3);
			compile();
		}

		public int run() {
			return update();
		}
	}

}
