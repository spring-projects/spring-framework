/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.List;
import java.util.ArrayList;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * @author Trevor Cook
 * @author Juergen Hoeller
 */
public class RdbmsOperationTests extends TestCase {

	public void testEmptySql() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		try {
			operation.compile();
			fail("Shouldn't allow compiling without sql statement");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testSetTypeAfterCompile() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.compile();
		try {
			operation.setTypes(new int[] {Types.INTEGER });
			fail("Shouldn't allow setting parameters after compile");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testDeclareParameterAfterCompile() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.compile();
		try {
			operation.declareParameter(new SqlParameter(Types.INTEGER));
			fail("Shouldn't allow setting parameters after compile");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testTooFewParameters() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setSql("select * from mytable");
		operation.setTypes(new int[] { Types.INTEGER });
		try {
			operation.validateParameters((Object[]) null);
			fail("Shouldn't validate without enough parameters");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testTooFewMapParameters() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setSql("select * from mytable");
		operation.setTypes(new int[] { Types.INTEGER });
		try {
			operation.validateNamedParameters((Map) null);
			fail("Shouldn't validate without enough parameters");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testOperationConfiguredViaJdbcTemplateMustGetDataSource() throws Exception {
		try {
			TestRdbmsOperation operation = new TestRdbmsOperation();
			operation.setSql("foo");
			operation.compile();
			fail("Can't compile without providing a DataSource for the JdbcTemplate");
		}
		catch (InvalidDataAccessApiUsageException ex) {
			// Check for helpful error message. Omit leading character
			// so as not to be fussy about case
			assertTrue(ex.getMessage().indexOf("ataSource") != -1);
		}
	}

	public void testTooManyParameters() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setSql("select * from mytable");
		try {
			operation.validateParameters(new Object[] {new Integer(1), new Integer(2)});
			fail("Shouldn't validate with too many parameters");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testUnspecifiedMapParameters() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setSql("select * from mytable");
		try {
			Map params = new HashMap();
			params.put("col1", "value");
			operation.validateNamedParameters(params);
			fail("Shouldn't validate with unspecified parameters");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testCompileTwice() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.setTypes(null);
		operation.compile();
		operation.compile();
	}

	public void testEmptyDataSource() {
		SqlOperation operation = new SqlOperation() {
		};
		operation.setSql("select * from mytable");
		try {
			operation.compile();
			fail("Shouldn't allow compiling without data source");
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			// OK
		}
	}

	public void testParameterPropagation() {
		SqlOperation operation = new SqlOperation() {
		};
		DataSource ds = new DriverManagerDataSource();
		operation.setDataSource(ds);
		operation.setFetchSize(10);
		operation.setMaxRows(20);
		JdbcTemplate jt = operation.getJdbcTemplate();
		assertEquals(ds, jt.getDataSource());
		assertEquals(10, jt.getFetchSize());
		assertEquals(20, jt.getMaxRows());
	}

	public void testValidateInOutParameter() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("DUMMY_PROC");
		operation.declareParameter(new SqlOutParameter("DUMMY_OUT_PARAM", Types.VARCHAR));
		operation.declareParameter(new SqlInOutParameter("DUMMY_IN_OUT_PARAM", Types.VARCHAR));
		operation.validateParameters(new Object[] {"DUMMY_VALUE1", "DUMMY_VALUE2"});
	}

	public void testParametersSetWithList() {
		TestRdbmsOperation operation = new TestRdbmsOperation();
		DataSource ds = new DriverManagerDataSource();
		operation.setDataSource(ds);
		operation.setSql("select * from mytable where one = ? and two = ?");
		List l = new ArrayList();
		l.add(new SqlParameter("one", Types.NUMERIC));
		l.add(new SqlParameter("two", Types.VARCHAR));
		operation.setParameters(new SqlParameter[] {
				new SqlParameter("one", Types.NUMERIC),
				new SqlParameter("two", Types.NUMERIC)});
		operation.afterPropertiesSet();
		try {
			operation.validateParameters(new Object[] {new Integer(1), new String("2")});
			assertEquals(2, operation.getDeclaredParameters().size());
			// OK
		}
		catch (InvalidDataAccessApiUsageException idaauex) {
			fail("Should have validated with parameters set using List: " + idaauex.getMessage());
		}
	}


	private static class TestRdbmsOperation extends RdbmsOperation {

		@Override
		protected void compileInternal() {
		}
	}

}
