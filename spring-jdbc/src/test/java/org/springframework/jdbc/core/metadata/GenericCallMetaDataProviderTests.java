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

package org.springframework.jdbc.core.metadata;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;
import org.springframework.util.function.ThrowingBiFunction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link GenericCallMetaDataProvider}.
 *
 * @author Stephane Nicoll
 */
class GenericCallMetaDataProviderTests {

	private final DatabaseMetaData databaseMetaData = mock(DatabaseMetaData.class);

	@Test
	void procedureNameWithNoMatch() throws SQLException {
		GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);

		ResultSet noProcedure = mockProcedures();
		given(this.databaseMetaData.getProcedures(null, null, "MY_PROCEDURE"))
				.willReturn(noProcedure);
		ResultSet noFunction = mockProcedures();
		given(this.databaseMetaData.getFunctions(null, null, "MY_PROCEDURE"))
				.willReturn(noFunction);

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> provider.initializeWithProcedureColumnMetaData(this.databaseMetaData, null, null, "my_procedure"))
				.withMessageContaining("'MY_PROCEDURE'");

		InOrder inOrder = inOrder(this.databaseMetaData);
		inOrder.verify(this.databaseMetaData).getUserName();
		inOrder.verify(this.databaseMetaData).getProcedures(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getFunctions(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getDatabaseProductName();
		verifyNoMoreInteractions(this.databaseMetaData);
	}

	@Test
	void procedureNameWithExactMatch() throws SQLException {
		GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);

		ResultSet myProcedure = mockProcedures(new Procedure(null, null, "MY_PROCEDURE"));
		given(this.databaseMetaData.getProcedures(null, null, "MY_PROCEDURE"))
				.willReturn(myProcedure);
		ResultSet myProcedureColumn = mockProcedureColumns("TEST", DatabaseMetaData.procedureColumnIn);
		given(this.databaseMetaData.getProcedureColumns(null, null, "MY_PROCEDURE", null))
				.willReturn(myProcedureColumn);

		provider.initializeWithProcedureColumnMetaData(this.databaseMetaData, null, null, "my_procedure");
		assertThat(provider.getCallParameterMetaData()).singleElement().satisfies(callParameterMetaData -> {
			assertThat(callParameterMetaData.getParameterName()).isEqualTo("TEST");
			assertThat(callParameterMetaData.getParameterType()).isEqualTo(DatabaseMetaData.procedureColumnIn);
		});

		InOrder inOrder = inOrder(this.databaseMetaData);
		inOrder.verify(this.databaseMetaData).getUserName();
		inOrder.verify(this.databaseMetaData).getProcedures(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getProcedureColumns(null, null, "MY_PROCEDURE", null);
		verifyNoMoreInteractions(this.databaseMetaData);
	}

	@Test
	void procedureNameWithSeveralMatchesFallBackOnEscaped() throws SQLException {
		GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);

		given(this.databaseMetaData.getSearchStringEscape()).willReturn("@");
		ResultSet myProcedures = mockProcedures(new Procedure(null, null, "MY_PROCEDURE"),
				new Procedure(null, null, "MYBPROCEDURE"));
		given(this.databaseMetaData.getProcedures(null, null, "MY_PROCEDURE"))
				.willReturn(myProcedures);
		ResultSet myProcedureEscaped = mockProcedures(new Procedure(null, null, "MY@_PROCEDURE"));
		given(this.databaseMetaData.getProcedures(null, null, "MY@_PROCEDURE"))
				.willReturn(myProcedureEscaped);
		ResultSet myProcedureColumn = mockProcedureColumns("TEST", DatabaseMetaData.procedureColumnIn);
		given(this.databaseMetaData.getProcedureColumns(null, null, "MY@_PROCEDURE", null))
				.willReturn(myProcedureColumn);

		provider.initializeWithProcedureColumnMetaData(this.databaseMetaData, null, null, "my_procedure");
		assertThat(provider.getCallParameterMetaData()).singleElement().satisfies(callParameterMetaData -> {
			assertThat(callParameterMetaData.getParameterName()).isEqualTo("TEST");
			assertThat(callParameterMetaData.getParameterType()).isEqualTo(DatabaseMetaData.procedureColumnIn);
		});

		InOrder inOrder = inOrder(this.databaseMetaData);
		inOrder.verify(this.databaseMetaData).getUserName();
		inOrder.verify(this.databaseMetaData).getProcedures(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getSearchStringEscape();
		inOrder.verify(this.databaseMetaData).getProcedures(null, null, "MY@_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getProcedureColumns(null, null, "MY@_PROCEDURE", null);
		verifyNoMoreInteractions(this.databaseMetaData);
	}

	@Test
	void procedureNameWithSeveralMatchesDoesNotFallBackOnEscapedIfEscapeCharacterIsNotAvailable() throws SQLException {
		given(this.databaseMetaData.getSearchStringEscape()).willReturn(null);
		GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);

		ResultSet myProcedures = mockProcedures(new Procedure(null, null, "MY_PROCEDURE"),
				new Procedure(null, null, "MYBPROCEDURE"));
		given(this.databaseMetaData.getProcedures(null, null, "MY_PROCEDURE"))
				.willReturn(myProcedures);

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> provider.initializeWithProcedureColumnMetaData(this.databaseMetaData, null, null, "my_procedure"))
				.withMessageContainingAll("'MY_PROCEDURE'", "null.null.MY_PROCEDURE", "null.null.MYBPROCEDURE");

		InOrder inOrder = inOrder(this.databaseMetaData);
		inOrder.verify(this.databaseMetaData).getUserName();
		inOrder.verify(this.databaseMetaData).getProcedures(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getSearchStringEscape();
		verifyNoMoreInteractions(this.databaseMetaData);
	}

	@Test
	void procedureNameWitNoMatchFallbackOnFunction() throws SQLException {
		GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);

		given(this.databaseMetaData.getSearchStringEscape()).willReturn("@");
		ResultSet noProcedure = mockProcedures();
		given(this.databaseMetaData.getProcedures(null, null, "MY_PROCEDURE"))
				.willReturn(noProcedure);
		ResultSet noProcedureWithEscaped = mockProcedures();
		given(this.databaseMetaData.getProcedures(null, null, "MY@_PROCEDURE"))
				.willReturn(noProcedureWithEscaped);
		ResultSet function = mockFunctions(new Procedure(null, null, "MY_PROCEDURE"));
		given(this.databaseMetaData.getFunctions(null, null, "MY_PROCEDURE"))
				.willReturn(function);
		ResultSet myProcedureColumn = mockProcedureColumns("TEST", DatabaseMetaData.procedureColumnIn);
		given(this.databaseMetaData.getFunctionColumns(null, null, "MY_PROCEDURE", null))
				.willReturn(myProcedureColumn);

		provider.initializeWithProcedureColumnMetaData(this.databaseMetaData, null, null, "my_procedure");
		assertThat(provider.getCallParameterMetaData()).singleElement().satisfies(callParameterMetaData -> {
			assertThat(callParameterMetaData.getParameterName()).isEqualTo("TEST");
			assertThat(callParameterMetaData.getParameterType()).isEqualTo(DatabaseMetaData.procedureColumnIn);
		});

		InOrder inOrder = inOrder(this.databaseMetaData);
		inOrder.verify(this.databaseMetaData).getUserName();
		inOrder.verify(this.databaseMetaData).getProcedures(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getFunctions(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getFunctionColumns(null, null, "MY_PROCEDURE", null);
		verifyNoMoreInteractions(this.databaseMetaData);
	}

	@Test
	void procedureNameWitNoMatchAndSeveralFunctionsFallbacksOnEscaped() throws SQLException {
		GenericCallMetaDataProvider provider = new GenericCallMetaDataProvider(this.databaseMetaData);

		given(this.databaseMetaData.getSearchStringEscape()).willReturn("@");
		ResultSet noProcedure = mockProcedures();
		given(this.databaseMetaData.getProcedures(null, null, "MY_PROCEDURE"))
				.willReturn(noProcedure);
		ResultSet functions = mockFunctions(new Procedure(null, null, "MY_PROCEDURE"),
				new Procedure(null, null, "MYBPROCEDURE"));
		given(this.databaseMetaData.getFunctions(null, null, "MY_PROCEDURE"))
				.willReturn(functions);
		ResultSet functionEscaped = mockFunctions(new Procedure(null, null, "MY@_PROCEDURE"));
		given(this.databaseMetaData.getFunctions(null, null, "MY@_PROCEDURE"))
				.willReturn(functionEscaped);
		ResultSet myProcedureColumn = mockProcedureColumns("TEST", DatabaseMetaData.procedureColumnIn);
		given(this.databaseMetaData.getFunctionColumns(null, null, "MY@_PROCEDURE", null))
				.willReturn(myProcedureColumn);

		provider.initializeWithProcedureColumnMetaData(this.databaseMetaData, null, null, "my_procedure");
		assertThat(provider.getCallParameterMetaData()).singleElement().satisfies(callParameterMetaData -> {
			assertThat(callParameterMetaData.getParameterName()).isEqualTo("TEST");
			assertThat(callParameterMetaData.getParameterType()).isEqualTo(DatabaseMetaData.procedureColumnIn);
		});

		InOrder inOrder = inOrder(this.databaseMetaData);
		inOrder.verify(this.databaseMetaData).getUserName();
		inOrder.verify(this.databaseMetaData).getProcedures(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getFunctions(null, null, "MY_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getSearchStringEscape();
		inOrder.verify(this.databaseMetaData).getFunctions(null, null, "MY@_PROCEDURE");
		inOrder.verify(this.databaseMetaData).getFunctionColumns(null, null, "MY@_PROCEDURE", null);
		verifyNoMoreInteractions(this.databaseMetaData);
	}

	private ResultSet mockProcedures(Procedure... procedures) {
		ResultSet resultSet = mock(ResultSet.class);
		List<Boolean> next = new ArrayList<>();
		Arrays.stream(procedures).forEach(p -> next.add(true));
		applyStrings(Arrays.stream(procedures).map(Procedure::catalog).toList(), (first, then) ->
				given(resultSet.getString("PROCEDURE_CAT")).willReturn(first, then));
		applyStrings(Arrays.stream(procedures).map(Procedure::schema).toList(), (first, then) ->
				given(resultSet.getString("PROCEDURE_SCHEM")).willReturn(first, then));
		applyStrings(Arrays.stream(procedures).map(Procedure::name).toList(), (first, then) ->
				given(resultSet.getString("PROCEDURE_NAME")).willReturn(first, then));
		next.add(false);
		applyBooleans(next, (first, then) -> given(resultSet.next()).willReturn(first, then));

		return resultSet;
	}

	private ResultSet mockFunctions(Procedure... procedures) {
		ResultSet resultSet = mock(ResultSet.class);
		List<Boolean> next = new ArrayList<>();
		Arrays.stream(procedures).forEach(p -> next.add(true));
		applyStrings(Arrays.stream(procedures).map(Procedure::catalog).toList(), (first, then) ->
				given(resultSet.getString("FUNCTION_CAT")).willReturn(first, then));
		applyStrings(Arrays.stream(procedures).map(Procedure::schema).toList(), (first, then) ->
				given(resultSet.getString("FUNCTION_SCHEM")).willReturn(first, then));
		applyStrings(Arrays.stream(procedures).map(Procedure::name).toList(), (first, then) ->
				given(resultSet.getString("FUNCTION_NAME")).willReturn(first, then));
		next.add(false);
		applyBooleans(next, (first, then) -> given(resultSet.next()).willReturn(first, then));

		return resultSet;
	}

	private ResultSet mockProcedureColumns(String columnName, int columnType) throws SQLException {
		ResultSet resultSet = mock(ResultSet.class);
		given(resultSet.next()).willReturn(true, false);
		given(resultSet.getString("COLUMN_NAME")).willReturn(columnName);
		given(resultSet.getInt("COLUMN_TYPE")).willReturn(columnType);
		return resultSet;
	}

	record Procedure(@Nullable String catalog, @Nullable String schema, String name) {

	}

	private void applyBooleans(List<Boolean> content, ThrowingBiFunction<Boolean, Boolean[], Object> split) {
		apply(content, Boolean[]::new, split);
	}

	private void applyStrings(List<String> content, ThrowingBiFunction<String, String[], Object> split) {
		apply(content, String[]::new, split);
	}

	private <T> void apply(List<T> content, IntFunction<T[]> generator, ThrowingBiFunction<T, T[], Object> split) {
		if (content.isEmpty()) {
			return;
		}
		T first = content.get(0);
		T[] array = content.subList(1, content.size()).toArray(generator);
		split.apply(first, array);
	}

}
