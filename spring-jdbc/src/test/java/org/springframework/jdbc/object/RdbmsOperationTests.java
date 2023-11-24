/*
 * Copyright 2002-2022 the original author or authors.
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

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Trevor Cook
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public class RdbmsOperationTests {

	private final TestRdbmsOperation operation = new TestRdbmsOperation();


	@Test
	public void emptySql() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(
				operation::compile);
	}

	@Test
	public void setTypeAfterCompile() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.compile();
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.setTypes(new int[] { Types.INTEGER }));
	}

	@Test
	public void declareParameterAfterCompile() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.compile();
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.declareParameter(new SqlParameter(Types.INTEGER)));
	}

	@Test
	public void tooFewParameters() {
		operation.setSql("select * from mytable");
		operation.setTypes(new int[] { Types.INTEGER });
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.validateParameters((Object[]) null));
	}

	@Test
	public void tooFewMapParameters() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		operation.setTypes(new int[] { Types.INTEGER });
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.validateNamedParameters((Map<String, String>) null));
	}

	@Test
	public void operationConfiguredViaJdbcTemplateMustGetDataSource() {
		operation.setSql("foo");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.compile())
			.withMessageContaining("'dataSource'");
	}

	@Test
	public void tooManyParameters() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.validateParameters(new Object[] { 1, 2 }));
	}
	@Test
	public void tooManyMapParameters() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.validateNamedParameters(Map.of("a", "b", "c", "d")));
	}

	@Test
	public void unspecifiedMapParameters() {
		operation.setDataSource(new DriverManagerDataSource());
		operation.setSql("select * from mytable");
		Map<String, String> params = new HashMap<>();
		params.put("col1", "value");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				operation.validateNamedParameters(params));
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
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(
				operation::compile);
	}

	@Test
	public void parameterPropagation() {
		SqlOperation operation = new SqlOperation() {};
		DataSource ds = new DriverManagerDataSource();
		operation.setDataSource(ds);
		operation.setFetchSize(10);
		operation.setMaxRows(20);
		JdbcTemplate jt = operation.getJdbcTemplate();
		assertThat(jt.getDataSource()).isEqualTo(ds);
		assertThat(jt.getFetchSize()).isEqualTo(10);
		assertThat(jt.getMaxRows()).isEqualTo(20);
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
		assertThat(operation.getDeclaredParameters()).hasSize(2);
	}


	private static class TestRdbmsOperation extends RdbmsOperation {

		@Override
		protected void compileInternal() {
		}
	}

}
