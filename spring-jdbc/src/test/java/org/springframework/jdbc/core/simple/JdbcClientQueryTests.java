/*
 * Copyright 2002-2024 the original author or authors.
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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Juergen Hoeller
 * @since 6.1
 */
class JdbcClientQueryTests {

	private DataSource dataSource = mock();

	private Connection connection = mock();

	private PreparedStatement preparedStatement = mock();

	private ResultSet resultSet = mock();

	private ResultSetMetaData resultSetMetaData = mock();

	private JdbcClient client = JdbcClient.create(dataSource);


	@BeforeEach
	void setup() throws Exception {
		given(dataSource.getConnection()).willReturn(connection);
		given(connection.prepareStatement(anyString())).willReturn(preparedStatement);
		given(preparedStatement.executeQuery()).willReturn(resultSet);
		given(resultSet.getMetaData()).willReturn(resultSetMetaData);
		given(resultSetMetaData.getColumnCount()).willReturn(1);
		given(resultSetMetaData.getColumnLabel(1)).willReturn("age");
	}


	// Indexed parameters

	@Test
	void queryForListWithIndexedParam() throws Exception {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getObject(1)).willReturn(11, 12);

		List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
				.param(3).query().listOfRows();

		assertThat(li.size()).as("All rows returned").isEqualTo(2);
		assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
		assertThat(li.get(1).get("age")).as("Second row is Integer").isEqualTo(12);

		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForListWithIndexedParamAndEmptyResult() throws Exception {
		given(resultSet.next()).willReturn(false);

		List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
				.param(3).query().listOfRows();

		assertThat(li.size()).as("All rows returned").isEqualTo(0);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForListWithIndexedParamAndSingleRow() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
				.param(3).query().listOfRows();

		assertThat(li.size()).as("All rows returned").isEqualTo(1);
		assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForMapWithIndexedParamAndSingleRow() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		Map<String, Object> map = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
				.param(1, 3)
				.query().singleRow();

		assertThat(map.get("age")).as("Row is Integer").isEqualTo(11);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForListWithIndexedParamAndSingleColumn() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		List<Object> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < ?")
				.param(1, 3)
				.query().singleColumn();

		assertThat(li).containsExactly(11);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithIndexedParamAndSingleValue() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(22);

		Integer value = (Integer) client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
				.param(1, 3)
				.query().singleValue();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithIndexedParamAndRowMapper() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
				.param(1, 3)
				.query((rs, rowNum) -> rs.getInt(1))
				.single();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForOptionalWithIndexedParamAndRowMapper() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Optional<Integer> value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
				.param(1, 3)
				.query((rs, rowNum) -> rs.getInt(1))
				.optional();

		assertThat(value.get()).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithIndexedParam() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
				.param(1, 3)
				.query(Integer.class).single();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntWithIndexedParam() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		int i = client.sql("SELECT AGE FROM CUSTMR WHERE ID = ?")
				.param(1, 3)
				.query(Integer.class).single();

		assertThat(i).as("Return of an int").isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForObjectWithIndexedParamAndList() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				client.sql("SELECT AGE FROM CUSTMR WHERE ID IN (?)").param(Arrays.asList(3, 4)).query().singleValue());
	}


	// Named parameters

	@Test
	void queryForListWithNamedParam() throws Exception {
		given(resultSet.next()).willReturn(true, true, false);
		given(resultSet.getObject(1)).willReturn(11, 12);

		List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
				.param("id", 3)
				.query().listOfRows();

		assertThat(li.size()).as("All rows returned").isEqualTo(2);
		assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
		assertThat(li.get(1).get("age")).as("Second row is Integer").isEqualTo(12);

		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForListWithNamedParamAndEmptyResult() throws Exception {
		given(resultSet.next()).willReturn(false);

		List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
				.param("id", 3)
				.query().listOfRows();

		assertThat(li.size()).as("All rows returned").isEqualTo(0);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForListWithNamedParamAndSingleRow() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		List<Map<String, Object>> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
				.param("id", 3)
				.query().listOfRows();

		assertThat(li.size()).as("All rows returned").isEqualTo(1);
		assertThat(li.get(0).get("age")).as("First row is Integer").isEqualTo(11);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForMapWithNamedParamAndSingleRow() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		Map<String, Object> map = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
				.param("id", 3)
				.query().singleRow();

		assertThat(map.get("age")).as("Row is Integer").isEqualTo(11);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForListWithNamedParamAndSingleColumn() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(11);

		List<Object> li = client.sql("SELECT AGE FROM CUSTMR WHERE ID < :id")
				.param("id", 3)
				.query().singleColumn();

		assertThat(li).containsExactly(11);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID < ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithNamedParamAndSingleValue() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getObject(1)).willReturn(22);

		Integer value = (Integer) client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.param("id", 3)
				.query().singleValue();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithNamedParamAndRowMapper() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.param("id", 3)
				.query((rs, rowNum) -> rs.getInt(1))
				.single();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForOptionalWithNamedParamAndRowMapper() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Optional<Integer> value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.param("id", 3)
				.query((rs, rowNum) -> rs.getInt(1))
				.optional();

		assertThat(value.get()).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithNamedParam() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.param("id", 3)
				.query(Integer.class).single();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithNamedParamAndList() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE ID IN (:ids)")
				.param("ids", Arrays.asList(3, 4))
				.query(Integer.class).single();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)");
		verify(preparedStatement).setObject(1, 3);
		verify(preparedStatement).setObject(2, 4);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntegerWithNamedParamAndListOfExpressionLists() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		List<Object[]> l1 = new ArrayList<>();
		l1.add(new Object[] {3, "Rod"});
		l1.add(new Object[] {4, "Juergen"});
		Integer value = client.sql("SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN (:multiExpressionList)")
				.param("multiExpressionList", l1)
				.query(Integer.class).single();

		assertThat(value).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE (ID, NAME) IN ((?, ?), (?, ?))");
		verify(preparedStatement).setObject(1, 3);
		verify(preparedStatement).setString(2, "Rod");
		verify(preparedStatement).setObject(3, 4);
		verify(preparedStatement).setString(4, "Juergen");
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForIntWithNamedParam() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		int i = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.param("id", 3)
				.query(Integer.class).single();

		assertThat(i).as("Return of an int").isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForLongWithParamBean() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getLong(1)).willReturn(87L);

		long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.paramSource(new ParameterBean(3))
				.query(Long.class).single();

		assertThat(l).as("Return of a long").isEqualTo(87);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3, Types.INTEGER);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForLongWithParamBeanWithCollection() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getLong(1)).willReturn(87L);

		long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID IN (:ids)")
				.paramSource(new ParameterCollectionBean(3, 5))
				.query(Long.class).single();

		assertThat(l).as("Return of a long").isEqualTo(87);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID IN (?, ?)");
		verify(preparedStatement).setObject(1, 3);
		verify(preparedStatement).setObject(2, 5);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForLongWithParamRecord() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getLong(1)).willReturn(87L);

		long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.paramSource(new ParameterRecord(3))
				.query(Long.class).single();

		assertThat(l).as("Return of a long").isEqualTo(87);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3, Types.INTEGER);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForLongWithParamFieldHolder() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getLong(1)).willReturn(87L);

		long l = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.paramSource(new ParameterFieldHolder(3))
				.query(Long.class).single();

		assertThat(l).as("Return of a long").isEqualTo(87);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3, Types.INTEGER);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForMappedRecordWithNamedParam() throws Exception {
		given(resultSet.findColumn("age")).willReturn(1);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		AgeRecord value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.param("id", 3)
				.query(AgeRecord.class).single();

		assertThat(value.age()).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}

	@Test
	void queryForMappedFieldHolderWithNamedParam() throws Exception {
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getInt(1)).willReturn(22);

		AgeFieldHolder value = client.sql("SELECT AGE FROM CUSTMR WHERE ID = :id")
				.param("id", 3)
				.query(AgeFieldHolder.class).single();

		assertThat(value.age).isEqualTo(22);
		verify(connection).prepareStatement("SELECT AGE FROM CUSTMR WHERE ID = ?");
		verify(preparedStatement).setObject(1, 3);
		verify(resultSet).close();
		verify(preparedStatement).close();
		verify(connection).close();
	}


	static class ParameterBean {

		private final int id;

		public ParameterBean(int id) {
			this.id = id;
		}

		public int getId() {
			return id;
		}
	}


	static class ParameterCollectionBean {

		private final Collection<Integer> ids;

		public ParameterCollectionBean(Integer... ids) {
			this.ids = Arrays.asList(ids);
		}

		public Collection<Integer> getIds() {
			return ids;
		}
	}


	record ParameterRecord(int id) {
	}


	static class ParameterFieldHolder {

		public ParameterFieldHolder(int id) {
			this.id = id;
		}

		public int id;
	}


	record AgeRecord(int age) {
	}


	static class AgeFieldHolder {

		public int age;
	}

}
