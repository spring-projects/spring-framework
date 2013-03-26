package org.springframework.jdbc.core.simple;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * Mock object based tests for CallMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class CallMetaDataContextTests {

	private DataSource dataSource;
	private Connection connection;
	private DatabaseMetaData databaseMetaData;

	private CallMetaDataContext context = new CallMetaDataContext();

	@Before
	public void setUp() throws Exception {
		connection = mock(Connection.class);
		databaseMetaData = mock(DatabaseMetaData.class);
		given(connection.getMetaData()).willReturn(databaseMetaData);
		dataSource = mock(DataSource.class);
		given(dataSource.getConnection()).willReturn(connection);
	}

	@After
	public void verifyClosed() throws Exception {
		verify(connection).close();
	}

	@Test
	public void testMatchParameterValuesAndSqlInOutParameters() throws Exception {
		final String TABLE = "customers";
		final String USER = "me";
		given(databaseMetaData.getDatabaseProductName()).willReturn("MyDB");
		given(databaseMetaData.getUserName()).willReturn(USER);
		given(databaseMetaData.storesLowerCaseIdentifiers()).willReturn(true);

		List<SqlParameter> parameters = new ArrayList<SqlParameter>();
		parameters.add(new SqlParameter("id", Types.NUMERIC));
		parameters.add(new SqlInOutParameter("name", Types.NUMERIC));
		parameters.add(new SqlOutParameter("customer_no", Types.NUMERIC));

		MapSqlParameterSource parameterSource = new MapSqlParameterSource();
		parameterSource.addValue("id", 1);
		parameterSource.addValue("name", "Sven");
		parameterSource.addValue("customer_no", "12345XYZ");

		context.setProcedureName(TABLE);
		context.initializeMetaData(dataSource);
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
