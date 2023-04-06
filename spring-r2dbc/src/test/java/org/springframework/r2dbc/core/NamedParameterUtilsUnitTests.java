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

package org.springframework.r2dbc.core;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import io.r2dbc.spi.Parameters;
import org.junit.jupiter.api.Test;

import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link NamedParameterUtils}.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Anton Naydenov
 */
public class NamedParameterUtilsUnitTests {

	private final BindMarkersFactory BIND_MARKERS = BindMarkersFactory.indexed("$", 1);


	@Test
	public void shouldParseSql() {
		String sql = "xxx :a yyyy :b :c :a zzzzz";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getParameterNames()).containsExactly("a", "b", "c", "a");
		assertThat(psql.getTotalParameterCount()).isEqualTo(4);
		assertThat(psql.getNamedParameterCount()).isEqualTo(3);

		String sql2 = "xxx &a yyyy ? zzzzz";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(psql2.getParameterNames()).containsExactly("a");
		assertThat(psql2.getTotalParameterCount()).isEqualTo(1);
		assertThat(psql2.getNamedParameterCount()).isEqualTo(1);

		String sql3 = "xxx &ä+:ö" + '\t' + ":ü%10 yyyy ? zzzzz";
		ParsedSql psql3 = NamedParameterUtils.parseSqlStatement(sql3);
		assertThat(psql3.getParameterNames()).containsExactly("ä", "ö", "ü");
	}

	@Test
	public void substituteNamedParameters() {
		MapBindParameterSource namedParams = new MapBindParameterSource(new HashMap<>());
		namedParams.addValue("a", "a").addValue("b", "b").addValue("c", "c");

		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				"xxx :a :b :c", BIND_MARKERS, namedParams);

		assertThat(operation.toQuery()).isEqualTo("xxx $1 $2 $3");

		PreparedOperation<?> operation2 = NamedParameterUtils.substituteNamedParameters(
				"xxx :a :b :c", BindMarkersFactory.named("@", "P", 8), namedParams);

		assertThat(operation2.toQuery()).isEqualTo("xxx @P0a @P1b @P2c");
	}

	@Test
	public void substituteObjectArray() {
		MapBindParameterSource namedParams = new MapBindParameterSource(new HashMap<>());
		namedParams.addValue("a", Arrays.asList(new Object[] { "Walter", "Heisenberg" },
				new Object[] { "Walt Jr.", "Flynn" }));

		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				"xxx :a", BIND_MARKERS, namedParams);

		assertThat(operation.toQuery()).isEqualTo("xxx ($1, $2), ($3, $4)");
	}

	@Test
	public void shouldBindObjectArray() {
		MapBindParameterSource namedParams = new MapBindParameterSource(new HashMap<>());
		namedParams.addValue("a", Arrays.asList(new Object[] { "Walter", "Heisenberg" },
				new Object[] { "Walt Jr.", "Flynn" }));

		BindTarget bindTarget = mock();

		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				"xxx :a", BIND_MARKERS, namedParams);
		operation.bindTo(bindTarget);

		verify(bindTarget).bind(0, "Walter");
		verify(bindTarget).bind(1, "Heisenberg");
		verify(bindTarget).bind(2, "Walt Jr.");
		verify(bindTarget).bind(3, "Flynn");
	}

	@Test
	public void parseSqlContainingComments() {
		String sql1 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX\n";

		ParsedSql psql1 = NamedParameterUtils.parseSqlStatement(sql1);
		assertThat(expand(psql1)).isEqualTo(
				"/*+ HINT */ xxx /* comment ? */ $1 yyyy $2 $3 $1 zzzzz -- :xx XX\n");

		MapBindParameterSource paramMap = new MapBindParameterSource(new HashMap<>());
		paramMap.addValue("a", "a");
		paramMap.addValue("b", "b");
		paramMap.addValue("c", "c");

		String sql2 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(expand(psql2)).isEqualTo(
				"/*+ HINT */ xxx /* comment ? */ $1 yyyy $2 $3 $1 zzzzz -- :xx XX");
	}

	@Test
	public void parseSqlStatementWithPostgresCasting() {
		String expectedSql = "select 'first name' from artists where id = $1 and birth_date=$2::timestamp";
		String sql = "select 'first name' from artists where id = :id and birth_date=:birthDate::timestamp";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				parsedSql, BIND_MARKERS, new MapBindParameterSource());

		assertThat(operation.toQuery()).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithPostgresContainedOperator() {
		String expectedSql = "select 'first name' from artists where info->'stat'->'albums' = ?? $1 and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		String sql = "select 'first name' from artists where info->'stat'->'albums' = ?? :album and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithPostgresAnyArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(0);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithPostgresAllArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND $1 = 'Back in Black'";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND :album = 'Back in Black'";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithEscapedColon() {
		String expectedSql = "select '0\\:0' as a, foo from bar where baz < DATE($1 23:59:59) and baz = $2";
		String sql = "select '0\\:0' as a, foo from bar where baz < DATE(:p1 23\\:59\\:59) and baz = :p2";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p1", "p2");
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithBracketDelimitedParameterNames() {
		String expectedSql = "select foo from bar where baz = b$1$2z";
		String sql = "select foo from bar where baz = b:{p1}:{p2}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p1", "p2");
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithEmptyBracketsOrBracketsInQuotes() {
		String expectedSql = "select foo from bar where baz = b:{}z";
		String sql = "select foo from bar where baz = b:{}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).isEmpty();
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);

		String expectedSql2 = "select foo from bar where baz = 'b:{p1}z'";
		String sql2 = "select foo from bar where baz = 'b:{p1}z'";

		ParsedSql parsedSql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(parsedSql2.getParameterNames()).isEmpty();
		assertThat(expand(parsedSql2)).isEqualTo(expectedSql2);
	}

	@Test
	public void parseSqlStatementWithSingleLetterInBrackets() {
		String expectedSql = "select foo from bar where baz = b$1z";
		String sql = "select foo from bar where baz = b:{p}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p");
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(expectedSql);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void substituteNamedParametersWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";

		assertThat(expand(expectedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void variableAssignmentOperator() {
		String expectedSql = "x := 1";

		assertThat(expand(expectedSql)).isEqualTo(expectedSql);
	}

	@Test
	public void parseSqlStatementWithQuotedSingleQuote() {
		String sql = "SELECT ':foo'':doo', :xxx FROM DUAL";

		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getTotalParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames()).containsExactly("xxx");
	}

	@Test
	public void parseSqlStatementWithQuotesAndCommentBefore() {
		String sql = "SELECT /*:doo*/':foo', :xxx FROM DUAL";

		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getTotalParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames()).containsExactly("xxx");
	}

	@Test
	public void parseSqlStatementWithQuotesAndCommentAfter() {
		String sql2 = "SELECT ':foo'/*:doo*/, :xxx FROM DUAL";

		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(psql2.getTotalParameterCount()).isEqualTo(1);
		assertThat(psql2.getParameterNames()).containsExactly("xxx");
	}

	@Test  // gh-27716
	public void parseSqlStatementWithSquareBracket() {
		String sql = "SELECT ARRAY[:ext]";

		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getNamedParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames()).containsExactly("ext");
	}

	@Test  // gh-27925
	void namedParamMapReference() {
		String sql = "insert into foos (id) values (:headers[id])";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getNamedParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames()).containsExactly("headers[id]");
	}

	@Test
	public void shouldAllowParsingMultipleUseOfParameter() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		ParsedSql parsed = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsed.getTotalParameterCount()).isEqualTo(2);
		assertThat(parsed.getNamedParameterCount()).isEqualTo(1);
		assertThat(parsed.getParameterNames()).containsExactly("id", "id");
	}

	@Test
	public void multipleEqualParameterReferencesBindsValueOnce() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		BindMarkersFactory factory = BindMarkersFactory.indexed("$", 0);

		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(
				sql, factory, new MapBindParameterSource(
						Collections.singletonMap("id", Parameters.in("foo"))));

		assertThat(operation.toQuery()).isEqualTo(
				"SELECT * FROM person where name = $0 or lastname = $0");

		operation.bindTo(new BindTarget() {
			@Override
			public void bind(String identifier, Object value) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bind(int index, Object value) {
				assertThat(index).isEqualTo(0);
				assertThat(value).isEqualTo(Parameters.in("foo"));
			}
			@Override
			public void bindNull(String identifier, Class<?> type) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bindNull(int index, Class<?> type) {
				throw new UnsupportedOperationException();
			}
		});
	}

	@Test
	public void multipleEqualCollectionParameterReferencesBindsValueOnce() {
		String sql = "SELECT * FROM person where name IN (:ids) or lastname IN (:ids)";

		BindMarkersFactory factory = BindMarkersFactory.indexed("$", 0);

		MultiValueMap<Integer, Object> bindings = new LinkedMultiValueMap<>();

		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(
				sql, factory, new MapBindParameterSource(Collections.singletonMap("ids",
						Parameters.in(Arrays.asList("foo", "bar", "baz")))));

		assertThat(operation.toQuery()).isEqualTo(
				"SELECT * FROM person where name IN ($0, $1, $2) or lastname IN ($0, $1, $2)");

		operation.bindTo(new BindTarget() {
			@Override
			public void bind(String identifier, Object value) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bind(int index, Object value) {
				assertThat(index).isIn(0, 1, 2);
				assertThat(value).isIn("foo", "bar", "baz");
				bindings.add(index, value);
			}
			@Override
			public void bindNull(String identifier, Class<?> type) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bindNull(int index, Class<?> type) {
				throw new UnsupportedOperationException();
			}
		});

		assertThat(bindings).containsEntry(0, Collections.singletonList("foo")) //
				.containsEntry(1, Collections.singletonList("bar")) //
				.containsEntry(2, Collections.singletonList("baz"));
	}

	@Test
	public void multipleEqualParameterReferencesForAnonymousMarkersBindsValueMultipleTimes() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		BindMarkersFactory factory = BindMarkersFactory.anonymous("?");

		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(
				sql, factory, new MapBindParameterSource(
						Collections.singletonMap("id", Parameters.in("foo"))));

		assertThat(operation.toQuery()).isEqualTo(
				"SELECT * FROM person where name = ? or lastname = ?");

		Map<Integer, Object> bindValues = new LinkedHashMap<>();

		operation.bindTo(new BindTarget() {
			@Override
			public void bind(String identifier, Object value) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bind(int index, Object value) {
				bindValues.put(index, value);
			}
			@Override
			public void bindNull(String identifier, Class<?> type) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bindNull(int index, Class<?> type) {
				throw new UnsupportedOperationException();
			}
		});

		assertThat(bindValues).hasSize(2).containsEntry(0, Parameters.in("foo")).containsEntry(1, Parameters.in("foo"));
	}

	@Test
	public void multipleEqualParameterReferencesBindsNullOnce() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		BindMarkersFactory factory = BindMarkersFactory.indexed("$", 0);

		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(
				sql, factory, new MapBindParameterSource(
						Collections.singletonMap("id", Parameters.in(String.class))));

		assertThat(operation.toQuery()).isEqualTo(
				"SELECT * FROM person where name = $0 or lastname = $0");

		operation.bindTo(new BindTarget() {
			@Override
			public void bind(String identifier, Object value) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bind(int index, Object value) {
				assertThat(index).isEqualTo(0);
				assertThat(value).isEqualTo(Parameters.in(String.class));
			}
			@Override
			public void bindNull(String identifier, Class<?> type) {
				throw new UnsupportedOperationException();
			}
			@Override
			public void bindNull(int index, Class<?> type) {
				throw new UnsupportedOperationException();
			}
		});
	}


	private String expand(ParsedSql sql) {
		return NamedParameterUtils.substituteNamedParameters(sql, BIND_MARKERS,
				new MapBindParameterSource()).toQuery();
	}

	private String expand(String sql) {
		return NamedParameterUtils.substituteNamedParameters(sql, BIND_MARKERS,
				new MapBindParameterSource()).toQuery();
	}

}
