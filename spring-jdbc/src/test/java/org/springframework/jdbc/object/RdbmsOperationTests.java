/*
 * Copyright 2002-2015 the original author or authors.
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

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.hamcrest.CoreMatchers.*;

import static org.junit.Assert.*;

/**
 * @author Trevor Cook
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class RdbmsOperationTests {

	private final TestRdbmsOperation operation = new TestRdbmsOperation();

	@Rule
	public final ExpectedException exception = ExpectedException.none();


	@Test
	public void emptySql() {
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.compile();
	}

	@Test
	public void setTypeAfterCompile() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.compile();
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.setTypes(new int[] { Types.INTEGER });
	}

	@Test
	public void declareParameterAfterCompile() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.compile();
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.declareParameter(new SqlParameter(Types.INTEGER));
	}

	@Test
	public void tooFewParameters() {
		operation.setSql("select * from mytable");
		operation.setTypes(new int[] { Types.INTEGER });
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.validateParameters((Object[]) null);
	}

	@Test
	public void tooFewMapParameters() {
		operation.setSql("select * from mytable");
		operation.setTypes(new int[] { Types.INTEGER });
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.validateNamedParameters((Map<String, String>) null);
	}

	@Test
	public void operationConfiguredViaJdbcTemplateMustGetDataSource() throws Exception {
		operation.setSql("foo");

		exception.expect(InvalidDataAccessApiUsageException.class);
		exception.expectMessage(containsString("ataSource"));
		operation.compile();
	}

	@Test
	public void tooManyParameters() {
		operation.setSql("select * from mytable");
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.validateParameters(new Object[] { 1, 2 });
	}

	@Test
	public void unspecifiedMapParameters() {
		operation.setSql("select * from mytable");
		Map<String, String> params = new HashMap<String, String>();
		params.put("col1", "value");
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.validateNamedParameters(params);
	}

	@Test
	public void compileTwice() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.setTypes(null);
		operation.compile();
		operation.compile();
	}

	@Test
	public void emptyDataSource() {
		SqlOperation operation = new SqlOperation() {};
		operation.setSql("select * from mytable");
		exception.expect(InvalidDataAccessApiUsageException.class);
		operation.compile();
	}

	@Test
	public void parameterPropagation() {
		SqlOperation operation = new SqlOperation() {};
		DataSource ds = new DriverManagerDataSource();
		operation.setDataSource(ds);
		operation.setFetchSize(10);
		operation.setMaxRows(20);
		JdbcTemplate jt = operation.getJdbcTemplate();
		assertEquals(ds, jt.getDataSource());
		assertEquals(10, jt.getFetchSize());
		assertEquals(20, jt.getMaxRows());
	}

	@Test
	public void validateInOutParameter() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("DUMMY_PROC");
		operation.declareParameter(new SqlOutParameter("DUMMY_OUT_PARAM", Types.VARCHAR));
		operation.declareParameter(new SqlInOutParameter("DUMMY_IN_OUT_PARAM", Types.VARCHAR));
		operation.validateParameters(new Object[] {"DUMMY_VALUE1", "DUMMY_VALUE2"});
	}

	@Test
	public void parametersSetWithList() {
		DataSource ds = new DriverManagerDataSource();
		operation.setDataSource(ds);
		operation.setSql("select * from mytable where one = ? and two = ?");
		operation.setParameters(new SqlParameter[] {
				new SqlParameter("one", Types.NUMERIC),
				new SqlParameter("two", Types.NUMERIC)});
		operation.afterPropertiesSet();
		operation.validateParameters(new Object[] { 1, "2" });
		assertEquals(2, operation.getDeclaredParameters().size());
	}


	private static class TestRdbmsOperation extends RdbmsOperation {

		@Override
		protected void compileInternal() {
		}
	}

}
