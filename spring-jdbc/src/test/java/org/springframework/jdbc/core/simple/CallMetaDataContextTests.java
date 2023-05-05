/*
 * Copyright 2002-2023 the original author or authors.
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
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.metadata.CallMetaDataContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Mock object based tests for CallMetaDataContext.
 *
 * @author Thomas Risberg
 */
public class CallMetaDataContextTests {

	private DataSource dataSource = mock();

	private Connection connection = mock();

	private DatabaseMetaData databaseMetaData = mock();

	private CallMetaDataContext context = new CallMetaDataContext();


	@BeforeEach
	public void setUp() throws Exception {
		given(connection.getMetaData()).willReturn(databaseMetaData);
		given(dataSource.getConnection()).willReturn(connection);
	}

	@AfterEach
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

		List<SqlParameter> parameters = new ArrayList<>();
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
		assertThat(inParameters).as("Wrong number of matched in parameter values").hasSize(2);
		assertThat(inParameters.containsKey("id")).as("in parameter value missing").isTrue();
		assertThat(inParameters.containsKey("name")).as("in out parameter value missing").isTrue();
		boolean condition = !inParameters.containsKey("customer_no");
		assertThat(condition).as("out parameter value matched").isTrue();

		List<String> names = context.getOutParameterNames();
		assertThat(names).as("Wrong number of out parameters").hasSize(2);

		List<SqlParameter> callParameters = context.getCallParameters();
		assertThat(callParameters).as("Wrong number of call parameters").hasSize(3);
	}

}
