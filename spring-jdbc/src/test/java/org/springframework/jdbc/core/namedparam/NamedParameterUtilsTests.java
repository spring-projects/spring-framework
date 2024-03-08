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

package org.springframework.jdbc.core.namedparam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlParameterValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link NamedParameterUtils}.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Artur Geraschenko
 * @author Yanming Zhou
 * @author Stephane Nicoll
 */
class NamedParameterUtilsTests {

	@Test
	void parseSql() {
		String sql = "xxx :a yyyy :b :c :a zzzzz";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo("xxx ? yyyy ? ? ? zzzzz");
		assertThat(parsedSql.getParameterNames()).containsExactly("a", "b", "c", "a");
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(4);
		assertThat(parsedSql.getNamedParameterCount()).isEqualTo(3);

		String sql2 = "xxx &a yyyy ? zzzzz";
		ParsedSql parsedSql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql2, null)).isEqualTo("xxx ? yyyy ? zzzzz");
		assertThat(parsedSql2.getParameterNames()).containsExactly("a");
		assertThat(parsedSql2.getTotalParameterCount()).isEqualTo(2);
		assertThat(parsedSql2.getNamedParameterCount()).isEqualTo(1);

		String sql3 = "xxx &ä+:ö" + '\t' + ":ü%10 yyyy ? zzzzz";
		ParsedSql parsedSql3 = NamedParameterUtils.parseSqlStatement(sql3);
		assertThat(parsedSql3.getParameterNames()).containsExactly("ä", "ö", "ü");
	}

	@Test
	void substituteNamedParameters() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a").addValue("b", "b").addValue("c", "c");
		assertThat(NamedParameterUtils.substituteNamedParameters("xxx :a :b :c", namedParams)).isEqualTo("xxx ? ? ?");
		assertThat(NamedParameterUtils.substituteNamedParameters("xxx :a :b :c xx :a :a", namedParams)).isEqualTo("xxx ? ? ? xx ? ?");
	}

	@Test
	void convertParamMapToArray() {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put("a", "a");
		paramMap.put("b", "b");
		paramMap.put("c", "c");
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :b :c", paramMap)).hasSize(3);
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :b :c xx :a :b", paramMap)).hasSize(5);
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :a :a xx :a :a", paramMap)).hasSize(5);
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :b :c xx :a :b", paramMap)[4]).isEqualTo("b");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).as("mixed named parameters and ? placeholders").isThrownBy(() ->
				NamedParameterUtils.buildValueArray("xxx :a :b ?", paramMap));
	}

	@Test
	void convertTypeMapToArray() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a", 1).addValue("b", "b", 2).addValue("c", "c", 3);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams)).hasSize(3);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams)).hasSize(5);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :a :a xx :a :a"), namedParams)).hasSize(5);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams)[4]).isEqualTo(2);
	}

	@Test
	void convertSqlParameterValueToArray() {
		SqlParameterValue sqlParameterValue = new SqlParameterValue(2, "b");
		Map<String, Object> paramMap = new HashMap<>();
		paramMap.put("a", "a");
		paramMap.put("b", sqlParameterValue);
		paramMap.put("c", "c");
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :b :c xx :a :b", paramMap)[4]).isSameAs(sqlParameterValue);
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a", 1).addValue("b", sqlParameterValue).addValue("c", "c", 3);
		assertThat(NamedParameterUtils
				.buildValueArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams, null)[4]).isSameAs(sqlParameterValue);
	}

	@Test
	void convertTypeMapToSqlParameterList() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a", 1).addValue("b", "b", 2).addValue("c", "c", 3, "SQL_TYPE");
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams)).hasSize(3);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams)).hasSize(5);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :a :a xx :a :a"), namedParams)).hasSize(5);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams).get(4).getSqlType()).isEqualTo(2);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams).get(2).getTypeName()).isEqualTo("SQL_TYPE");
	}

	@Test
	void buildValueArrayWithMissingParameterValue() {
		String sql = "select count(0) from foo where id = :id";
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				NamedParameterUtils.buildValueArray(sql, Collections.emptyMap()));
	}

	@Test
	void substituteNamedParametersWithStringContainingQuotes() {
		String expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'";
		String sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'";
		String newSql = NamedParameterUtils.substituteNamedParameters(sql, new MapSqlParameterSource());
		assertThat(newSql).isEqualTo(expectedSql);
	}

	@Test
	void testParseSqlStatementWithStringContainingQuotes() {
		String expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'";
		String sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo(expectedSql);
	}

	@Test  // SPR-4789
	void parseSqlContainingComments() {
		String sql1 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX\n";
		ParsedSql parsedSql1 = NamedParameterUtils.parseSqlStatement(sql1);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql1, null)).isEqualTo("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX\n");
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("a", "a");
		paramMap.addValue("b", "b");
		paramMap.addValue("c", "c");
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql1, paramMap, null);
		assertThat(params).containsExactly("a", "b", "c", "a");

		String sql2 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX";
		ParsedSql parsedSql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql2, null)).isEqualTo("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX");

		String sql3 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz /* :xx XX*";
		ParsedSql parsedSql3 = NamedParameterUtils.parseSqlStatement(sql3);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql3, null)).isEqualTo("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz /* :xx XX*");

		String sql4 = "/*+ HINT */ xxx /* comment :a ? */ :a yyyy :b :c :a zzzzz /* :xx XX*";
		ParsedSql parsedSql4 = NamedParameterUtils.parseSqlStatement(sql4);
		Map<String, String> parameters = Collections.singletonMap("a", "0");
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql4, new MapSqlParameterSource(parameters))).isEqualTo("/*+ HINT */ xxx /* comment :a ? */ ? yyyy ? ? ? zzzzz /* :xx XX*");
	}

	@Test  // SPR-4612
	void parseSqlStatementWithPostgresCasting() {
		String expectedSql = "select 'first name' from artists where id = ? and birth_date=?::timestamp";
		String sql = "select 'first name' from artists where id = :id and birth_date=:birthDate::timestamp";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo(expectedSql);
	}

	@Test  // SPR-13582
	void parseSqlStatementWithPostgresContainedOperator() {
		String expectedSql = "select 'first name' from artists where info->'stat'->'albums' = ?? ? and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		String sql = "select 'first name' from artists where info->'stat'->'albums' = ?? :album and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo(expectedSql);
	}

	@Test  // SPR-15382
	void parseSqlStatementWithPostgresAnyArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(0);
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo(expectedSql);
	}

	@Test  // SPR-15382
	void parseSqlStatementWithPostgresAllArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND ? = 'Back in Black'";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND :album = 'Back in Black'";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo(expectedSql);
	}

	@Test  // SPR-7476
	void parseSqlStatementWithEscapedColon() {
		String expectedSql = "select '0\\:0' as a, foo from bar where baz < DATE(? 23:59:59) and baz = ?";
		String sql = "select '0\\:0' as a, foo from bar where baz < DATE(:p1 23\\:59\\:59) and baz = :p2";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p1", "p2");
		String finalSql = substituteNamedParameters(parsedSql);
		assertThat(finalSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-7476
	void parseSqlStatementWithBracketDelimitedParameterNames() {
		String expectedSql = "select foo from bar where baz = b??z";
		String sql = "select foo from bar where baz = b:{p1}:{p2}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p1", "p2");
		String finalSql = substituteNamedParameters(parsedSql);
		assertThat(finalSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-7476
	void parseSqlStatementWithEmptyBracketsOrBracketsInQuotes() {
		String expectedSql = "select foo from bar where baz = b:{}z";
		String sql = "select foo from bar where baz = b:{}z";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).isEmpty();
		String finalSql = substituteNamedParameters(parsedSql);
		assertThat(finalSql).isEqualTo(expectedSql);

		String expectedSql2 = "select foo from bar where baz = 'b:{p1}z'";
		String sql2 = "select foo from bar where baz = 'b:{p1}z'";

		ParsedSql parsedSql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(parsedSql2.getParameterNames()).isEmpty();
		String finalSql2 = NamedParameterUtils.substituteNamedParameters(parsedSql2, null);
		assertThat(finalSql2).isEqualTo(expectedSql2);
	}

	@Test
	void parseSqlStatementWithSingleLetterInBrackets() {
		String expectedSql = "select foo from bar where baz = b?z";
		String sql = "select foo from bar where baz = b:{p}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("p");
		String finalSql = substituteNamedParameters(parsedSql);
		assertThat(finalSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-2544
	void parseSqlStatementWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(expectedSql);
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo(expectedSql);
	}

	@Test  // SPR-2544
	void substituteNamedParametersWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";
		String newSql = NamedParameterUtils.substituteNamedParameters(expectedSql, new MapSqlParameterSource());
		assertThat(newSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-3173
	void variableAssignmentOperator() {
		String expectedSql = "x := 1";
		String newSql = NamedParameterUtils.substituteNamedParameters(expectedSql, new MapSqlParameterSource());
		assertThat(newSql).isEqualTo(expectedSql);
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
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getNamedParameterCount()).isEqualTo(1);
		assertThat(parsedSql.getParameterNames()).containsExactly("ext");

		String sqlToUse = substituteNamedParameters(parsedSql);
		assertThat(sqlToUse).isEqualTo("SELECT ARRAY[?]");
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
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getNamedParameterCount()).isEqualTo(1);
		assertThat(parsedSql.getParameterNames()).containsExactly("headers[id]");

		class Foo {
			final Map<String, Object> headers = new HashMap<>();

			public Foo() {
				this.headers.put("id", 1);
			}

			public Map<String, Object> getHeaders() {
				return this.headers;
			}
		}

		Foo foo = new Foo();
		SqlParameterSource paramSource = new BeanPropertySqlParameterSource(foo);
		Object[] params = NamedParameterUtils.buildValueArray(parsedSql, paramSource, null);
		assertThat(params[0]).isInstanceOfSatisfying(SqlParameterValue.class, sqlParameterValue ->
				assertThat(sqlParameterValue.getValue()).isEqualTo(foo.getHeaders().get("id")));

		String sqlToUse = NamedParameterUtils.substituteNamedParameters(parsedSql, paramSource);
		assertThat(sqlToUse).isEqualTo("insert into foos (id) values (?)");
	}

	@Test // gh-31944
	void parseSqlStatementWithBackticks() {
		String sql = "select * from `tb&user` where id = :id";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames()).containsExactly("id");
		assertThat(substituteNamedParameters(parsedSql)).isEqualTo("select * from `tb&user` where id = ?");
	}

	private static String substituteNamedParameters(ParsedSql parsedSql) {
		return NamedParameterUtils.substituteNamedParameters(parsedSql, null);
	}

}
