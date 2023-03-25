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

package org.springframework.jdbc.core.namedparam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.SqlParameterValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Artur Geraschenko
 * @author Yanming Zhou
 */
public class NamedParameterUtilsTests {

	@Test
	public void parseSql() {
		String sql = "xxx :a yyyy :b :c :a zzzzz";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(NamedParameterUtils.substituteNamedParameters(psql, null)).isEqualTo("xxx ? yyyy ? ? ? zzzzz");
		assertThat(psql.getParameterNames().get(0)).isEqualTo("a");
		assertThat(psql.getParameterNames().get(2)).isEqualTo("c");
		assertThat(psql.getParameterNames().get(3)).isEqualTo("a");
		assertThat(psql.getTotalParameterCount()).isEqualTo(4);
		assertThat(psql.getNamedParameterCount()).isEqualTo(3);

		String sql2 = "xxx &a yyyy ? zzzzz";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(NamedParameterUtils.substituteNamedParameters(psql2, null)).isEqualTo("xxx ? yyyy ? zzzzz");
		assertThat(psql2.getParameterNames().get(0)).isEqualTo("a");
		assertThat(psql2.getTotalParameterCount()).isEqualTo(2);
		assertThat(psql2.getNamedParameterCount()).isEqualTo(1);

		String sql3 = "xxx &ä+:ö" + '\t' + ":ü%10 yyyy ? zzzzz";
		ParsedSql psql3 = NamedParameterUtils.parseSqlStatement(sql3);
		assertThat(psql3.getParameterNames().get(0)).isEqualTo("ä");
		assertThat(psql3.getParameterNames().get(1)).isEqualTo("ö");
		assertThat(psql3.getParameterNames().get(2)).isEqualTo("ü");
	}

	@Test
	public void substituteNamedParameters() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a").addValue("b", "b").addValue("c", "c");
		assertThat(NamedParameterUtils.substituteNamedParameters("xxx :a :b :c", namedParams)).isEqualTo("xxx ? ? ?");
		assertThat(NamedParameterUtils.substituteNamedParameters("xxx :a :b :c xx :a :a", namedParams)).isEqualTo("xxx ? ? ? xx ? ?");
	}

	@Test
	public void convertParamMapToArray() {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put("a", "a");
		paramMap.put("b", "b");
		paramMap.put("c", "c");
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :b :c", paramMap).length).isSameAs(3);
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :b :c xx :a :b", paramMap).length).isSameAs(5);
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :a :a xx :a :a", paramMap).length).isSameAs(5);
		assertThat(NamedParameterUtils.buildValueArray("xxx :a :b :c xx :a :b", paramMap)[4]).isEqualTo("b");
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).as("mixed named parameters and ? placeholders").isThrownBy(() ->
				NamedParameterUtils.buildValueArray("xxx :a :b ?", paramMap));
	}

	@Test
	public void convertTypeMapToArray() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a", 1).addValue("b", "b", 2).addValue("c", "c", 3);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams).length).isSameAs(3);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams).length).isSameAs(5);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :a :a xx :a :a"), namedParams).length).isSameAs(5);
		assertThat(NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams)[4]).isEqualTo(2);
	}

	@Test
	public void convertSqlParameterValueToArray() {
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
	public void convertTypeMapToSqlParameterList() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a", 1).addValue("b", "b", 2).addValue("c", "c", 3, "SQL_TYPE");
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams).size()).isSameAs(3);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams).size()).isSameAs(5);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :a :a xx :a :a"), namedParams).size()).isSameAs(5);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams).get(4).getSqlType()).isEqualTo(2);
		assertThat(NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams).get(2).getTypeName()).isEqualTo("SQL_TYPE");
	}

	@Test
	public void buildValueArrayWithMissingParameterValue() {
		String sql = "select count(0) from foo where id = :id";
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() ->
				NamedParameterUtils.buildValueArray(sql, Collections.<String, Object>emptyMap()));
	}

	@Test
	public void substituteNamedParametersWithStringContainingQuotes() {
		String expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'";
		String sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'";
		String newSql = NamedParameterUtils.substituteNamedParameters(sql, new MapSqlParameterSource());
		assertThat(newSql).isEqualTo(expectedSql);
	}

	@Test
	public void testParseSqlStatementWithStringContainingQuotes() {
		String expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'";
		String sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql, null)).isEqualTo(expectedSql);
	}

	@Test  // SPR-4789
	public void parseSqlContainingComments() {
		String sql1 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX\n";
		ParsedSql psql1 = NamedParameterUtils.parseSqlStatement(sql1);
		assertThat(NamedParameterUtils.substituteNamedParameters(psql1, null)).isEqualTo("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX\n");
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("a", "a");
		paramMap.addValue("b", "b");
		paramMap.addValue("c", "c");
		Object[] params = NamedParameterUtils.buildValueArray(psql1, paramMap, null);
		assertThat(params.length).isEqualTo(4);
		assertThat(params[0]).isEqualTo("a");
		assertThat(params[1]).isEqualTo("b");
		assertThat(params[2]).isEqualTo("c");
		assertThat(params[3]).isEqualTo("a");

		String sql2 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(NamedParameterUtils.substituteNamedParameters(psql2, null)).isEqualTo("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX");

		String sql3 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz /* :xx XX*";
		ParsedSql psql3 = NamedParameterUtils.parseSqlStatement(sql3);
		assertThat(NamedParameterUtils.substituteNamedParameters(psql3, null)).isEqualTo("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz /* :xx XX*");

		String sql4 = "/*+ HINT */ xxx /* comment :a ? */ :a yyyy :b :c :a zzzzz /* :xx XX*";
		ParsedSql psql4 = NamedParameterUtils.parseSqlStatement(sql4);
		Map<String, String> parameters = Collections.singletonMap("a", "0");
		assertThat(NamedParameterUtils.substituteNamedParameters(psql4, new MapSqlParameterSource(parameters))).isEqualTo("/*+ HINT */ xxx /* comment :a ? */ ? yyyy ? ? ? zzzzz /* :xx XX*");
	}

	@Test  // SPR-4612
	public void parseSqlStatementWithPostgresCasting() {
		String expectedSql = "select 'first name' from artists where id = ? and birth_date=?::timestamp";
		String sql = "select 'first name' from artists where id = :id and birth_date=:birthDate::timestamp";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql, null)).isEqualTo(expectedSql);
	}

	@Test  // SPR-13582
	public void parseSqlStatementWithPostgresContainedOperator() {
		String expectedSql = "select 'first name' from artists where info->'stat'->'albums' = ?? ? and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		String sql = "select 'first name' from artists where info->'stat'->'albums' = ?? :album and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql, null)).isEqualTo(expectedSql);
	}

	@Test  // SPR-15382
	public void parseSqlStatementWithPostgresAnyArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(0);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql, null)).isEqualTo(expectedSql);
	}

	@Test  // SPR-15382
	public void parseSqlStatementWithPostgresAllArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND ? = 'Back in Black'";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND :album = 'Back in Black'";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getTotalParameterCount()).isEqualTo(1);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql, null)).isEqualTo(expectedSql);
	}

	@Test  // SPR-7476
	public void parseSqlStatementWithEscapedColon() {
		String expectedSql = "select '0\\:0' as a, foo from bar where baz < DATE(? 23:59:59) and baz = ?";
		String sql = "select '0\\:0' as a, foo from bar where baz < DATE(:p1 23\\:59\\:59) and baz = :p2";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames().size()).isEqualTo(2);
		assertThat(parsedSql.getParameterNames().get(0)).isEqualTo("p1");
		assertThat(parsedSql.getParameterNames().get(1)).isEqualTo("p2");
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertThat(finalSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-7476
	public void parseSqlStatementWithBracketDelimitedParameterNames() {
		String expectedSql = "select foo from bar where baz = b??z";
		String sql = "select foo from bar where baz = b:{p1}:{p2}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames().size()).isEqualTo(2);
		assertThat(parsedSql.getParameterNames().get(0)).isEqualTo("p1");
		assertThat(parsedSql.getParameterNames().get(1)).isEqualTo("p2");
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertThat(finalSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-7476
	public void parseSqlStatementWithEmptyBracketsOrBracketsInQuotes() {
		String expectedSql = "select foo from bar where baz = b:{}z";
		String sql = "select foo from bar where baz = b:{}z";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames().size()).isEqualTo(0);
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertThat(finalSql).isEqualTo(expectedSql);

		String expectedSql2 = "select foo from bar where baz = 'b:{p1}z'";
		String sql2 = "select foo from bar where baz = 'b:{p1}z'";

		ParsedSql parsedSql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(parsedSql2.getParameterNames().size()).isEqualTo(0);
		String finalSql2 = NamedParameterUtils.substituteNamedParameters(parsedSql2, null);
		assertThat(finalSql2).isEqualTo(expectedSql2);
	}

	@Test
	public void parseSqlStatementWithSingleLetterInBrackets() {
		String expectedSql = "select foo from bar where baz = b?z";
		String sql = "select foo from bar where baz = b:{p}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(parsedSql.getParameterNames().size()).isEqualTo(1);
		assertThat(parsedSql.getParameterNames().get(0)).isEqualTo("p");
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertThat(finalSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-2544
	public void parseSqlStatementWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(expectedSql);
		assertThat(NamedParameterUtils.substituteNamedParameters(parsedSql, null)).isEqualTo(expectedSql);
	}

	@Test  // SPR-2544
	public void substituteNamedParametersWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";
		String newSql = NamedParameterUtils.substituteNamedParameters(expectedSql, new MapSqlParameterSource());
		assertThat(newSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-3173
	public void variableAssignmentOperator() {
		String expectedSql = "x := 1";
		String newSql = NamedParameterUtils.substituteNamedParameters(expectedSql, new MapSqlParameterSource());
		assertThat(newSql).isEqualTo(expectedSql);
	}

	@Test  // SPR-8280
	public void parseSqlStatementWithQuotedSingleQuote() {
		String sql = "SELECT ':foo'':doo', :xxx FROM DUAL";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getTotalParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames().get(0)).isEqualTo("xxx");
	}

	@Test
	public void parseSqlStatementWithQuotesAndCommentBefore() {
		String sql = "SELECT /*:doo*/':foo', :xxx FROM DUAL";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getTotalParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames().get(0)).isEqualTo("xxx");
	}

	@Test
	public void parseSqlStatementWithQuotesAndCommentAfter() {
		String sql2 = "SELECT ':foo'/*:doo*/, :xxx FROM DUAL";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertThat(psql2.getTotalParameterCount()).isEqualTo(1);
		assertThat(psql2.getParameterNames().get(0)).isEqualTo("xxx");
	}

	@Test  // gh-27925
	void namedParamMapReference() {
		String sql = "insert into foos (id) values (:headers[id])";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertThat(psql.getNamedParameterCount()).isEqualTo(1);
		assertThat(psql.getParameterNames()).containsExactly("headers[id]");

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
		Object[] params = NamedParameterUtils.buildValueArray(psql,
				new BeanPropertySqlParameterSource(foo), null);

		assertThat(params[0]).isInstanceOf(SqlParameterValue.class);
		assertThat(((SqlParameterValue) params[0]).getValue()).isEqualTo(foo.getHeaders().get("id"));
	}

}
