package org.springframework.jdbc.core.simple;

import junit.framework.TestCase;
import org.easymock.MockControl;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlInOutParameter;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock object based tests for CallMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class CallMetaDataContextTests extends TestCase {
	private MockControl ctrlDataSource;
	private DataSource mockDataSource;
	private MockControl ctrlConnection;
	private Connection mockConnection;
	private MockControl ctrlDatabaseMetaData;
	private DatabaseMetaData mockDatabaseMetaData;

	private CallMetaDataContext context = new CallMetaDataContext();

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

	public void testMatchParameterValuesAndSqlInOutParameters() throws Exception {
		final String TABLE = "customers";
		final String USER = "me";

		mockDatabaseMetaData.getDatabaseProductName();
		ctrlDatabaseMetaData.setReturnValue("MyDB");
		mockDatabaseMetaData.supportsCatalogsInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.supportsSchemasInProcedureCalls();
		ctrlDatabaseMetaData.setReturnValue(false);

		mockDatabaseMetaData.getUserName();
		ctrlDatabaseMetaData.setReturnValue(USER);
		mockDatabaseMetaData.storesUpperCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(false);
		mockDatabaseMetaData.storesLowerCaseIdentifiers();
		ctrlDatabaseMetaData.setReturnValue(true);

		replay();

		List<SqlParameter> parameters = new ArrayList<SqlParameter>();
		parameters.add(new SqlParameter("id", Types.NUMERIC));
		parameters.add(new SqlInOutParameter("name", Types.NUMERIC));
		parameters.add(new SqlOutParameter("customer_no", Types.NUMERIC));

		MapSqlParameterSource parameterSource = new MapSqlParameterSource();
		parameterSource.addValue("id", 1);
		parameterSource.addValue("name", "Sven");
		parameterSource.addValue("customer_no", "12345XYZ");

		context.setProcedureName(TABLE);
		context.initializeMetaData(mockDataSource);
		context.processParameters(parameters);

		Map<String, Object> inParameters = context.matchInParameterValuesWithCallParameters(parameterSource);
		assertEquals("Wrong number of matched in parameter values", 2, inParameters.size());
		assertTrue("in parameter value missing", inParameters.containsKey("id"));
		assertTrue("in out parameter value missing", inParameters.containsKey("name"));
		assertTrue("out parameter value matched", !inParameters.containsKey("customer_no"));

		List<String> names = context.getOutParameterNames();
		assertEquals("Wrong number of out parameters", 2, names.size());

		List<SqlParameter> callParameters = context.getCallParameters();
		assertEquals("Wrong number of call parameters", 3, callParameters.size());

	}

}
