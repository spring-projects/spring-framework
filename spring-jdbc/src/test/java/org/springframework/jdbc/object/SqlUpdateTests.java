/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.jdbc.object;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.easymock.MockControl;
import org.apache.commons.logging.LogFactory;

import org.springframework.jdbc.AbstractJdbcTests;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * @author Trevor Cook
 * @author Thomas Risberg
 * @author Juergen Hoeller
 */
public class SqlUpdateTests extends AbstractJdbcTests {

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

	private final boolean debugEnabled = LogFactory.getLog(JdbcTemplate.class).isDebugEnabled();

	private MockControl ctrlPreparedStatement;
	private PreparedStatement mockPreparedStatement;
	private MockControl ctrlResultSet;
	private ResultSet mockResultSet;
	private MockControl ctrlResultSetMetaData;
	private ResultSetMetaData mockResultSetMetaData;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		ctrlPreparedStatement = MockControl.createControl(PreparedStatement.class);
		mockPreparedStatement = (PreparedStatement) ctrlPreparedStatement.getMock();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		if (shouldVerify()) {
			ctrlPreparedStatement.verify();
		}
	}

	@Override
	protected void replay() {
		super.replay();
		ctrlPreparedStatement.replay();
	}


	public void testUpdate() throws SQLException {
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		Updater pc = new Updater();
		int rowsAffected = pc.run();
		assertEquals(1, rowsAffected);
	}

	public void testUpdateInt() throws SQLException {
		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE_INT);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		IntUpdater pc = new IntUpdater();
		int rowsAffected = pc.run(1);
		assertEquals(1, rowsAffected);
	}

	public void testUpdateIntInt() throws SQLException {
		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		mockPreparedStatement.setObject(2, new Integer(1), Types.NUMERIC);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE_INT_INT);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		IntIntUpdater pc = new IntIntUpdater();
		int rowsAffected = pc.run(1, 1);
		assertEquals(1, rowsAffected);
	}

	public void testNamedParameterUpdateWithUnnamedDeclarations() throws SQLException {
		doTestNamedParameterUpdate(false);
	}

	public void testNamedParameterUpdateWithNamedDeclarations() throws SQLException {
		doTestNamedParameterUpdate(true);
	}

	private void doTestNamedParameterUpdate(final boolean namedDeclarations) throws SQLException {
		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		mockPreparedStatement.setObject(2, new Integer(1), Types.DECIMAL);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE_INT_INT);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		class NamedParameterUpdater extends SqlUpdate {

			public NamedParameterUpdater() {
				setSql(UPDATE_NAMED_PARAMETERS);
				setDataSource(mockDataSource);
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
				Map params = new HashMap();
				params.put("perfId", new Integer(performanceId));
				params.put("priceId", new Integer(type));
				return updateByNamedParam(params);
			}
		}

		NamedParameterUpdater pc = new NamedParameterUpdater();
		int rowsAffected = pc.run(1, 1);
		assertEquals(1, rowsAffected);
	}

	public void testUpdateString() throws SQLException {
		mockPreparedStatement.setString(1, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE_STRING);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		StringUpdater pc = new StringUpdater();
		int rowsAffected = pc.run("rod");
		assertEquals(1, rowsAffected);
	}

	public void testUpdateMixed() throws SQLException {
		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		mockPreparedStatement.setObject(2, new Integer(1), Types.NUMERIC, 2);
		mockPreparedStatement.setString(3, "rod");
		mockPreparedStatement.setObject(4, Boolean.TRUE, Types.BOOLEAN);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE_OBJECTS);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		MixedUpdater pc = new MixedUpdater();
		int rowsAffected = pc.run(1, 1, "rod", true);
		assertEquals(1, rowsAffected);
	}

	public void testUpdateAndGeneratedKeys() throws SQLException {
		ctrlResultSetMetaData = MockControl.createControl(ResultSetMetaData.class);
		mockResultSetMetaData = (ResultSetMetaData) ctrlResultSetMetaData.getMock();
		mockResultSetMetaData.getColumnCount();
		ctrlResultSetMetaData.setReturnValue(1);
		mockResultSetMetaData.getColumnLabel(1);
		ctrlResultSetMetaData.setReturnValue("1", 2);

		ctrlResultSet = MockControl.createControl(ResultSet.class);
		mockResultSet = (ResultSet) ctrlResultSet.getMock();
		mockResultSet.getMetaData();
		ctrlResultSet.setReturnValue(mockResultSetMetaData);
		mockResultSet.next();
		ctrlResultSet.setReturnValue(true);
		mockResultSet.getObject(1);
		ctrlResultSet.setReturnValue(new Integer(11));
		mockResultSet.next();
		ctrlResultSet.setReturnValue(false);
		mockResultSet.close();
		ctrlResultSet.setVoidCallable();

		mockPreparedStatement.setString(1, "rod");
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		mockPreparedStatement.getGeneratedKeys();
		ctrlPreparedStatement.setReturnValue(mockResultSet);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(INSERT_GENERATE_KEYS, PreparedStatement.RETURN_GENERATED_KEYS);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();
		ctrlResultSet.replay();
		ctrlResultSetMetaData.replay();

		GeneratedKeysUpdater pc = new GeneratedKeysUpdater();
		KeyHolder generatedKeyHolder = new GeneratedKeyHolder();
		int rowsAffected = pc.run("rod", generatedKeyHolder);
		assertEquals(1, rowsAffected);
		assertEquals(1, generatedKeyHolder.getKeyList().size());
		assertEquals(11, generatedKeyHolder.getKey().intValue());
	}

	public void testUpdateConstructor() throws SQLException {
		mockPreparedStatement.setObject(1, new Integer(1), Types.NUMERIC);
		mockPreparedStatement.setObject(2, new Integer(1), Types.NUMERIC);
		mockPreparedStatement.setString(3, "rod");
		mockPreparedStatement.setObject(4, Boolean.TRUE, Types.BOOLEAN);
		ctrlPreparedStatement.setVoidCallable();
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(1);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE_OBJECTS);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		ConstructorUpdater pc = new ConstructorUpdater();
		int rowsAffected = pc.run(1, 1, "rod", true);
		assertEquals(1, rowsAffected);
	}

	public void testUnderMaxRows() throws SQLException {
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(3);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		MaxRowsUpdater pc = new MaxRowsUpdater();
		int rowsAffected = pc.run();
		assertEquals(3, rowsAffected);
	}

	public void testMaxRows() throws SQLException {
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(5);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		MaxRowsUpdater pc = new MaxRowsUpdater();
		int rowsAffected = pc.run();
		assertEquals(5, rowsAffected);
	}

	public void testOverMaxRows() throws SQLException {
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(8);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		MaxRowsUpdater pc = new MaxRowsUpdater();
		try {
			int rowsAffected = pc.run();
			fail("Shouldn't continue when too many rows affected");
		}
		catch (JdbcUpdateAffectedIncorrectNumberOfRowsException juaicrex) {
			// OK
		}
	}

	public void testRequiredRows() throws SQLException {
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(3);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		RequiredRowsUpdater pc = new RequiredRowsUpdater();
		int rowsAffected = pc.run();
		assertEquals(3, rowsAffected);
	}

	public void testNotRequiredRows() throws SQLException {
		mockPreparedStatement.executeUpdate();
		ctrlPreparedStatement.setReturnValue(2);
		if (debugEnabled) {
			mockPreparedStatement.getWarnings();
			ctrlPreparedStatement.setReturnValue(null);
		}
		mockPreparedStatement.close();
		ctrlPreparedStatement.setVoidCallable();

		mockConnection.prepareStatement(UPDATE);
		ctrlConnection.setReturnValue(mockPreparedStatement);

		replay();

		RequiredRowsUpdater pc = new RequiredRowsUpdater();
		try {
			int rowsAffected = pc.run();
			fail("Shouldn't continue when too many rows affected");
		}
		catch (JdbcUpdateAffectedIncorrectNumberOfRowsException juaicrex) {
			// OK
		}
	}


	private class Updater extends SqlUpdate {

		public Updater() {
			setSql(UPDATE);
			setDataSource(mockDataSource);
			compile();
		}

		public int run() {
			return update();
		}
	}


	private class IntUpdater extends SqlUpdate {

		public IntUpdater() {
			setSql(UPDATE_INT);
			setDataSource(mockDataSource);
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
			setDataSource(mockDataSource);
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
			setDataSource(mockDataSource);
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
			setDataSource(mockDataSource);
			declareParameter(new SqlParameter(Types.NUMERIC));
			declareParameter(new SqlParameter(Types.NUMERIC, 2));
			declareParameter(new SqlParameter(Types.VARCHAR));
			declareParameter(new SqlParameter(Types.BOOLEAN));
			compile();
		}

		public int run(int performanceId, int type, String name, boolean confirmed) {
			Object[] params =
				new Object[] {new Integer(performanceId), new Integer(type), name,
											new Boolean(confirmed)};
			return update(params);
		}
	}


	private class GeneratedKeysUpdater extends SqlUpdate {

		public GeneratedKeysUpdater() {
			setSql(INSERT_GENERATE_KEYS);
			setDataSource(mockDataSource);
			declareParameter(new SqlParameter(Types.VARCHAR));
			setReturnGeneratedKeys(true);
			compile();
		}

		public int run(String name, KeyHolder generatedKeyHolder) {
			Object[] params = new Object[] {name};
			return update(params, generatedKeyHolder);
		}
	}


	private class ConstructorUpdater extends SqlUpdate {

		public ConstructorUpdater() {
			super(mockDataSource, UPDATE_OBJECTS,
					new int[] {Types.NUMERIC, Types.NUMERIC, Types.VARCHAR, Types.BOOLEAN });
			compile();
		}

		public int run(int performanceId, int type, String name, boolean confirmed) {
			Object[] params =
				new Object[] {
					new Integer(performanceId), new Integer(type), name, new Boolean(confirmed)};
			return update(params);
		}
	}


	private class MaxRowsUpdater extends SqlUpdate {

		public MaxRowsUpdater() {
			setSql(UPDATE);
			setDataSource(mockDataSource);
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
			setDataSource(mockDataSource);
			setRequiredRowsAffected(3);
			compile();
		}

		public int run() {
			return update();
		}
	}

}
