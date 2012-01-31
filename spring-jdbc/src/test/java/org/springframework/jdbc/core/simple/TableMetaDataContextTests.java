package org.springframework.jdbc.core.simple;

import junit.framework.TestCase;
import org.springframework.jdbc.core.metadata.TableMetaDataContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.SqlParameterValue;
import org.easymock.MockControl;

import javax.sql.DataSource;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.sql.Types;
import java.sql.DatabaseMetaData;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Mock object based tests for TableMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class TableMetaDataContextTests extends TestCase {
	private MockControl ctrlDataSource;
	private DataSource mockDataSource;
	private MockControl ctrlConnection;
	private Connection mockConnection;
	private MockControl ctrlDatabaseMetaData;
	private DatabaseMetaData mockDatabaseMetaData;

	private TableMetaDataContext context = new TableMetaDataContext();

	protected void setUp() throws Exception {
		super.setUp();

		ctrlDatabaseMetaData = MockControl.createControl(DatabaseMetaData.class);
		mockDatabaseMetaData = (DatabaseMetaData) ctrlDatabaseMetaData.getMock();

		ctrlConnection = MockControl.createControl(Connection.class);
		mockConnection = (Connection) ctrlConnection.getMock();
		mockConnection.getMetaData();
		ctrlConnection.setDefaultReturnValue(mockDatabaseMetaData);
		mockConnection.close();
		ctrlConnection.setDefaultVoidCallable();

		ctrlDataSource = MockControl.createControl(DataSource.class);
		mockDataSource = (DataSource) ctrlDataSource.getMock();
		mockDataSource.getConnection();
		ctrlDataSource.setDefaultReturnValue(mockConnection);

	}

	protected void tearDown() throws Exception {
		super.tearDown();
		ctrlDatabaseMetaData.verify();
		ctrlDataSource.verify();
	}

	protected void replay() {
		ctrlDatabaseMetaData.replay();
		ctrlConnection.replay();
		ctrlDataSource.replay();
	}

	public void testMatchInParametersAndSqlTypeInfoWrapping() throws Exception {
		final String TABLE = "customers";
		final String USER = "me";

		MockControl ctrlMetaDataResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockMetaDataResultSet = (ResultSet) ctrlMetaDataResultSet.getMock();
		mockMetaDataResultSet.next();
		ctrlMetaDataResultSet.setReturnValue(true);
		mockMetaDataResultSet.getString("TABLE_CAT");
		ctrlMetaDataResultSet.setReturnValue(null);
		mockMetaDataResultSet.getString("TABLE_SCHEM");
		ctrlMetaDataResultSet.setReturnValue(USER);
		mockMetaDataResultSet.getString("TABLE_NAME");
		ctrlMetaDataResultSet.setReturnValue(TABLE);
		mockMetaDataResultSet.getString("TABLE_TYPE");
		ctrlMetaDataResultSet.setReturnValue("TABLE");
		mockMetaDataResultSet.next();
		ctrlMetaDataResultSet.setReturnValue(false);
		mockMetaDataResultSet.close();
		ctrlMetaDataResultSet.setVoidCallable();

		MockControl ctrlColumnsResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockColumnsResultSet = (ResultSet) ctrlColumnsResultSet.getMock();
		mockColumnsResultSet.next();
		ctrlColumnsResultSet.setReturnValue(true);
		mockColumnsResultSet.getString("COLUMN_NAME");
		ctrlColumnsResultSet.setReturnValue("id");
		mockColumnsResultSet.getInt("DATA_TYPE");
		ctrlColumnsResultSet.setReturnValue(Types.INTEGER);
		mockColumnsResultSet.getBoolean("NULLABLE");
		ctrlColumnsResultSet.setReturnValue(false);
		mockColumnsResultSet.next();
		ctrlColumnsResultSet.setReturnValue(true);
		mockColumnsResultSet.getString("COLUMN_NAME");
		ctrlColumnsResultSet.setReturnValue("name");
		mockColumnsResultSet.getInt("DATA_TYPE");
		ctrlColumnsResultSet.setReturnValue(Types.VARCHAR);
		mockColumnsResultSet.getBoolean("NULLABLE");
		ctrlColumnsResultSet.setReturnValue(true);
		mockColumnsResultSet.next();
		ctrlColumnsResultSet.setReturnValue(true);
		mockColumnsResultSet.getString("COLUMN_NAME");
		ctrlColumnsResultSet.setReturnValue("customersince");
		mockColumnsResultSet.getInt("DATA_TYPE");
		ctrlColumnsResultSet.setReturnValue(Types.DATE);
		mockColumnsResultSet.getBoolean("NULLABLE");
		ctrlColumnsResultSet.setReturnValue(true);
		mockColumnsResultSet.next();
		ctrlColumnsResultSet.setReturnValue(true);
		mockColumnsResultSet.getString("COLUMN_NAME");
		ctrlColumnsResultSet.setReturnValue("version");
		mockColumnsResultSet.getInt("DATA_TYPE");
		ctrlColumnsResultSet.setReturnValue(Types.NUMERIC);
		mockColumnsResultSet.getBoolean("NULLABLE");
		ctrlColumnsResultSet.setReturnValue(false);
		mockColumnsResultSet.next();
		ctrlColumnsResultSet.setReturnValue(false);
		mockColumnsResultSet.close();
		ctrlColumnsResultSet.setVoidCallable();

		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.supportsGetGeneratedKeys();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.getDatabaseProductVersion();
		ctrlDatabaseMetaData.setReturnValue("1.0");
		mockDatabaseMetaData.getUserName();
		ctrlDatabaseMetaData.setReturnValue(USER);
		mockDatabaseMetaData.storesUpperCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesLowerCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(true);
		mockDatabaseMetaData.getTables(null, null, TABLE, null);
		ctrlDatabaseMetaData.setReturnValue(mockMetaDataResultSet);
		mockDatabaseMetaData.getColumns(null, USER, TABLE, null);
		ctrlDatabaseMetaData.setReturnValue(mockColumnsResultSet);

		ctrlMetaDataResultSet.replay();
		ctrlColumnsResultSet.replay();
		replay();
		
		MapSqlParameterSource map = new MapSqlParameterSource();
		map.addValue("id", 1);
		map.addValue("name", "Sven");
		map.addValue("customersince", new Date());
		map.addValue("version", 0);		
		map.registerSqlType("customersince", Types.DATE);
		map.registerSqlType("version", Types.NUMERIC);

		context.setTableName(TABLE);
		context.processMetaData(mockDataSource, new ArrayList<String>(), new String[] {});

		List<Object> values = context.matchInParameterValuesWithInsertColumns(map);

		assertEquals("wrong number of parameters: ", 4, values.size());
		assertTrue("id not wrapped with type info", values.get(0) instanceof Number);
		assertTrue("name not wrapped with type info", values.get(1) instanceof String);
		assertTrue("date wrapped with type info", values.get(2) instanceof SqlParameterValue);
		assertTrue("version wrapped with type info", values.get(3) instanceof SqlParameterValue);
	}

	public void testTableWithSingleColumnGeneratedKey() throws Exception {
		final String TABLE = "customers";
		final String USER = "me";

		MockControl ctrlMetaDataResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockMetaDataResultSet = (ResultSet) ctrlMetaDataResultSet.getMock();
		mockMetaDataResultSet.next();
		ctrlMetaDataResultSet.setReturnValue(true);
		mockMetaDataResultSet.getString("TABLE_CAT");
		ctrlMetaDataResultSet.setReturnValue(null);
		mockMetaDataResultSet.getString("TABLE_SCHEM");
		ctrlMetaDataResultSet.setReturnValue(USER);
		mockMetaDataResultSet.getString("TABLE_NAME");
		ctrlMetaDataResultSet.setReturnValue(TABLE);
		mockMetaDataResultSet.getString("TABLE_TYPE");
		ctrlMetaDataResultSet.setReturnValue("TABLE");
		mockMetaDataResultSet.next();
		ctrlMetaDataResultSet.setReturnValue(false);
		mockMetaDataResultSet.close();
		ctrlMetaDataResultSet.setVoidCallable();

		MockControl ctrlColumnsResultSet = MockControl.createControl(ResultSet.class);
		ResultSet mockColumnsResultSet = (ResultSet) ctrlColumnsResultSet.getMock();
		mockColumnsResultSet.next();
		ctrlColumnsResultSet.setReturnValue(true);
		mockColumnsResultSet.getString("COLUMN_NAME");
		ctrlColumnsResultSet.setReturnValue("id");
		mockColumnsResultSet.getInt("DATA_TYPE");
		ctrlColumnsResultSet.setReturnValue(Types.INTEGER);
		mockColumnsResultSet.getBoolean("NULLABLE");
		ctrlColumnsResultSet.setReturnValue(false);
		mockColumnsResultSet.next();
		ctrlColumnsResultSet.setReturnValue(false);
		mockColumnsResultSet.close();
		ctrlColumnsResultSet.setVoidCallable();

		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.supportsGetGeneratedKeys();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.getDatabaseProductVersion();
		ctrlDatabaseMetaData.setReturnValue("1.0");
		mockDatabaseMetaData.getUserName();
		ctrlDatabaseMetaData.setReturnValue(USER);
		mockDatabaseMetaData.storesUpperCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesLowerCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(true);
		mockDatabaseMetaData.getTables(null, null, TABLE, null);
		ctrlDatabaseMetaData.setReturnValue(mockMetaDataResultSet);
		mockDatabaseMetaData.getColumns(null, USER, TABLE, null);
		ctrlDatabaseMetaData.setReturnValue(mockColumnsResultSet);

		ctrlMetaDataResultSet.replay();
		ctrlColumnsResultSet.replay();
		replay();

		MapSqlParameterSource map = new MapSqlParameterSource();

		String[] keyCols = new String[] {"id"};

		context.setTableName(TABLE);
		context.processMetaData(mockDataSource, new ArrayList<String>(), keyCols);

		List<Object> values = context.matchInParameterValuesWithInsertColumns(map);

		String insertString = context.createInsertString(keyCols);

		assertEquals("wrong number of parameters: ", 0, values.size());
		assertEquals("empty insert not generated correctly", "INSERT INTO customers () VALUES()", insertString);
	}
}
