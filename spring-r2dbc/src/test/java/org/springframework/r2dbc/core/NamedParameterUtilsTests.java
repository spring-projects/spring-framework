/*
 * Copyright 2002-present the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.r2dbc.spi.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NamedParameterUtils}.
 *
 * @author Mark Paluch
 * @author Jens Schauder
 * @author Anton Naydenov
 * @author Sam Brannen
 */
class NamedParameterUtilsTests {

	private static final BindMarkersFactory INDEXED_MARKERS = BindMarkersFactory.indexed("$", 1);

	private static final BindMarkersFactory ANONYMOUS_MARKERS = BindMarkersFactory.anonymous("?");


	@Test
	void shouldParseSql() {
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
	void substituteNamedParameters() {
		MapBindParameterSource namedParams = new MapBindParameterSource(new HashMap<>());
		namedParams.addValue("a", "a").addValue("b", "b").addValue("c", "c");

		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				"xxx :a :b :c", INDEXED_MARKERS, namedParams);

		assertThat(operation.toQuery()).isEqualTo("xxx $1 $2 $3");

		PreparedOperation<?> operation2 = NamedParameterUtils.substituteNamedParameters(
				"xxx :a :b :c", BindMarkersFactory.named("@", "P", 8), namedParams);

		assertThat(operation2.toQuery()).isEqualTo("xxx @P0a @P1b @P2c");
	}

	@Test
	void substituteObjectArray() {
		MapBindParameterSource namedParams = new MapBindParameterSource(new HashMap<>());
		namedParams.addValue("a",
				List.of(new Object[] {"Walter", "Heisenberg"},
						new Object[] {"Walt Jr.", "Flynn"}));

		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				"xxx :a", INDEXED_MARKERS, namedParams);

		assertThat(operation.toQuery()).isEqualTo("xxx ($1, $2), ($3, $4)");
	}

	@Test
	void shouldBindObjectArray() {
		MapBindParameterSource namedParams = new MapBindParameterSource(new HashMap<>());
		namedParams.addValue("a",
				List.of(new Object[] {"Walter", "Heisenberg"},
						new Object[] {"Walt Jr.", "Flynn"}));

		BindTarget bindTarget = mock();

		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				"xxx :a", INDEXED_MARKERS, namedParams);
		operation.bindTo(bindTarget);

		verify(bindTarget).bind(0, "Walter");
		verify(bindTarget).bind(1, "Heisenberg");
		verify(bindTarget).bind(2, "Walt Jr.");
		verify(bindTarget).bind(3, "Flynn");
	}

	@Test
	void parseSqlContainingComments() {
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
	void parseSqlStatementWithPostgresCasting() {
		String expectedSql = "select 'first name' from artists where id = $1 and birth_date=$2::timestamp";
		String sql = "select 'first name' from artists where id = :id and birth_date=:birthDate::timestamp";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		PreparedOperation<?> operation = NamedParameterUtils.substituteNamedParameters(
				parsedSql, INDEXED_MARKERS, new MapBindParameterSource());

		assertThat(operation.toQuery()).isEqualTo(expectedSql);
	}

	@Test
	void parseSqlStatementWithPostgresContainedOperator() {
		String expectedSql = "select 'first name' from artists where info->'stat'->'albums' = ?? $1 and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		String sql = "select 'first name' from artists where info->'stat'->'albums' = ?? :album and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	void parseSqlStatementWithPostgresAnyArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(0);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	void parseSqlStatementWithPostgresAllArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND $1 = 'Back in Black'";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND :album = 'Back in Black'";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	void parseSqlStatementWithEscapedColon() {
		String expectedSql = "select '0\\:0' as a, foo from bar where baz < DATE($1 23:59:59) and baz = $2";
		String sql = "select '0\\:0' as a, foo from bar where baz < DATE(:p1 23\\:59\\:59) and baz = :p2";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p1", "p2");
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	void parseSqlStatementWithBracketDelimitedParameterNames() {
		String expectedSql = "select foo from bar where baz = b$1$2z";
		String sql = "select foo from bar where baz = b:{p1}:{p2}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p1", "p2");
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	void parseSqlStatementWithEmptyBracketsOrBracketsInQuotes() {
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
	void parseSqlStatementWithSingleLetterInBrackets() {
		String expectedSql = "select foo from bar where baz = b$1z";
		String sql = "select foo from bar where baz = b:{p}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p");
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	void parseSqlStatementWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(expectedSql);
		assertThat(expand(parsedSql)).isEqualTo(expectedSql);
	}

	@Test
	void substituteNamedParametersWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";

		assertThat(expand(expectedSql)).isEqualTo(expectedSql);
	}

	@Test
	void variableAssignmentOperator() {
		String expectedSql = "x := 1";

		assertThat(expand(expectedSql)).isEqualTo(expectedSql);
	}

	@ParameterizedTest // SPR-8280 and others
	@ValueSource(strings = {
			"SELECT ':foo'':doo', :xxx FROM DUAL",
			"SELECT /*:doo*/':foo', :xxx FROM DUAL",
			"SELECT ':foo'/*:doo*/, :xxx FROM DUAL",
			"SELECT \":foo\"\":doo\", :xxx FROM DUAL",
			"SELECT `:foo``:doo`, :xxx FROM DUAL"
		})
	void parseSqlStatementWithParametersInsideQuotesAndComments(String sql) {
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(parsedSql.getParameterNames()).containsExactly("xxx");
	}

	@Test  // gh-27716
	void parseSqlStatementWithSquareBracket() {
		String sql = "SELECT ARRAY[:ext]";

		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getNamedParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames()).containsExactly("ext");

		assertThat(expand(psql)).isEqualTo("SELECT ARRAY[$1]");
	}

	@Test  // gh-31596
	void paramNameWithNestedSquareBrackets() {
		String sql = "insert into GeneratedAlways (id, first_name, last_name) values " +
				"(:records[0].id, :records[0].firstName, :records[0].lastName), " +
				"(:records[1].id, :records[1].firstName, :records[1].lastName)";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsOnly(
				"records[0].id", "records[0].firstName", "records[0].lastName",
				"records[1].id", "records[1].firstName", "records[1].lastName");
	}

	@Test  // gh-27925
	void namedParamMapReference() {
		String sql = "insert into foos (id) values (:headers[id])";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getNamedParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames()).containsExactly("headers[id]");
	}

	@Test  // gh-31944 / gh-32285
	void parseSqlStatementWithBackticks() {
		String sql = "select * from `tb&user` where id = :id";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("id");
		assertThat(expand(parsedSql)).isEqualTo("select * from `tb&user` where id = $1");
	}

	@Test
	void shouldAllowParsingMultipleUseOfParameter() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		ParsedSql parsed = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsed.getTotalParameterCount()).isEqualTo(2);
		assertThat(parsed.getNamedParameterCount()).isEqualTo(1);
		assertThat(parsed.getParameterNames()).containsExactly("id", "id");
	}

	@Test
	void multipleEqualParameterReferencesBindsValueOnce() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		MapBindParameterSource source = new MapBindParameterSource(Map.of("id", Parameters.in("foo")));
		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(sql, INDEXED_MARKERS, source);

		assertThat(operation.toQuery())
				.isEqualTo("SELECT * FROM person where name = $1 or lastname = $1");

		TrackingBindTarget trackingBindTarget = new TrackingBindTarget();

		operation.bindTo(trackingBindTarget);

		assertThat(trackingBindTarget.bindings)
				.hasSize(1)
				.containsEntry(0, Parameters.in("foo"));
	}

	@Test
	void multipleEqualCollectionParameterReferencesForIndexedMarkersBindsValuesOnce() {
		String sql = "SELECT * FROM person where name IN (:ids) or lastname IN (:ids)";

		MapBindParameterSource source = new MapBindParameterSource(Map.of("ids",
				Parameters.in(List.of("foo", "bar", "baz"))));
		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(sql, INDEXED_MARKERS, source);

		assertThat(operation.toQuery())
				.isEqualTo("SELECT * FROM person where name IN ($1, $2, $3) or lastname IN ($1, $2, $3)");

		TrackingBindTarget trackingBindTarget = new TrackingBindTarget();

		operation.bindTo(trackingBindTarget);

		assertThat(trackingBindTarget.bindings)
				.hasSize(3)
				.containsEntry(0, "foo")
				.containsEntry(1, "bar")
				.containsEntry(2, "baz");
	}

	@Test  // gh-34768
	void multipleEqualCollectionParameterReferencesForAnonymousMarkersBindsValuesTwice() {
		String sql = "SELECT * FROM fund_info WHERE fund_code IN (:fundCodes) OR fund_code IN (:fundCodes)";

		MapBindParameterSource source = new MapBindParameterSource(Map.of("fundCodes", Parameters.in(List.of("foo", "bar", "baz"))));
		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(sql, ANONYMOUS_MARKERS, source);

		assertThat(operation.toQuery())
				.isEqualTo("SELECT * FROM fund_info WHERE fund_code IN (?, ?, ?) OR fund_code IN (?, ?, ?)");

		TrackingBindTarget trackingBindTarget = new TrackingBindTarget();

		operation.bindTo(trackingBindTarget);

		assertThat(trackingBindTarget.bindings)
				.hasSize(6)
				.containsEntry(0, "foo")
				.containsEntry(1, "bar")
				.containsEntry(2, "baz")
				.containsEntry(3, "foo")
				.containsEntry(4, "bar")
				.containsEntry(5, "baz");
	}

	@Test
	void multipleEqualParameterReferencesForAnonymousMarkersBindsValueTwice() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		MapBindParameterSource source = new MapBindParameterSource(Map.of("id", Parameters.in("foo")));
		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(sql, ANONYMOUS_MARKERS, source);

		assertThat(operation.toQuery())
				.isEqualTo("SELECT * FROM person where name = ? or lastname = ?");

		TrackingBindTarget trackingBindTarget = new TrackingBindTarget();

		operation.bindTo(trackingBindTarget);

		assertThat(trackingBindTarget.bindings)
				.hasSize(2)
				.containsEntry(0, Parameters.in("foo"))
				.containsEntry(1, Parameters.in("foo"));
	}

	@Test
	void multipleEqualParameterReferencesBindsNullOnce() {
		String sql = "SELECT * FROM person where name = :id or lastname = :id";

		MapBindParameterSource source = new MapBindParameterSource(Map.of("id", Parameters.in(String.class)));
		PreparedOperation<String> operation = NamedParameterUtils.substituteNamedParameters(sql, INDEXED_MARKERS, source);

		assertThat(operation.toQuery())
				.isEqualTo("SELECT * FROM person where name = $1 or lastname = $1");

		TrackingBindTarget trackingBindTarget = new TrackingBindTarget();

		operation.bindTo(trackingBindTarget);

		assertThat(trackingBindTarget.bindings)
				.hasSize(1)
				.containsEntry(0, Parameters.in(String.class));
	}


	private static String expand(ParsedSql sql) {
		return NamedParameterUtils.substituteNamedParameters(sql, INDEXED_MARKERS,
				new MapBindParameterSource()).toQuery();
	}

	private static String expand(String sql) {
		return NamedParameterUtils.substituteNamedParameters(sql, INDEXED_MARKERS,
				new MapBindParameterSource()).toQuery();
	}


	private static class TrackingBindTarget implements BindTarget {

		final Map<Integer, Object> bindings = new HashMap<>();

		@Override
		public void bind(String identifier, Object value) {}

		@Override
		public void bind(int index, Object value) {
			this.bindings.put(index, value);
		}

		@Override
		public void bindNull(String identifier, Class<?> type) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void bindNull(int index, Class<?> type) {
			throw new UnsupportedOperationException();
		}
	}

}
