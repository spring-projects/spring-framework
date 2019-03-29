/*
 * Copyright 2002-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.junit.Assert.*;

/**
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @author Rick Evans
 * @author Artur Geraschenko
 */
public class NamedParameterUtilsTests {

	@Test
	public void parseSql() {
		String sql = "xxx :a yyyy :b :c :a zzzzz";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals("xxx ? yyyy ? ? ? zzzzz", NamedParameterUtils.substituteNamedParameters(psql, null));
		assertEquals("a", psql.getParameterNames().get(0));
		assertEquals("c", psql.getParameterNames().get(2));
		assertEquals("a", psql.getParameterNames().get(3));
		assertEquals(4, psql.getTotalParameterCount());
		assertEquals(3, psql.getNamedParameterCount());

		String sql2 = "xxx &a yyyy ? zzzzz";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertEquals("xxx ? yyyy ? zzzzz", NamedParameterUtils.substituteNamedParameters(psql2, null));
		assertEquals("a", psql2.getParameterNames().get(0));
		assertEquals(2, psql2.getTotalParameterCount());
		assertEquals(1, psql2.getNamedParameterCount());

		String sql3 = "xxx &ä+:ö" + '\t' + ":ü%10 yyyy ? zzzzz";
		ParsedSql psql3 = NamedParameterUtils.parseSqlStatement(sql3);
		assertEquals("ä", psql3.getParameterNames().get(0));
		assertEquals("ö", psql3.getParameterNames().get(1));
		assertEquals("ü", psql3.getParameterNames().get(2));
	}

	@Test
	public void substituteNamedParameters() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a").addValue("b", "b").addValue("c", "c");
		assertEquals("xxx ? ? ?", NamedParameterUtils.substituteNamedParameters("xxx :a :b :c", namedParams));
		assertEquals("xxx ? ? ? xx ? ?",
				NamedParameterUtils.substituteNamedParameters("xxx :a :b :c xx :a :a", namedParams));
	}

	@Test
	public void convertParamMapToArray() {
		Map<String, String> paramMap = new HashMap<>();
		paramMap.put("a", "a");
		paramMap.put("b", "b");
		paramMap.put("c", "c");
		assertSame(3, NamedParameterUtils.buildValueArray("xxx :a :b :c", paramMap).length);
		assertSame(5, NamedParameterUtils.buildValueArray("xxx :a :b :c xx :a :b", paramMap).length);
		assertSame(5, NamedParameterUtils.buildValueArray("xxx :a :a :a xx :a :a", paramMap).length);
		assertEquals("b", NamedParameterUtils.buildValueArray("xxx :a :b :c xx :a :b", paramMap)[4]);
		try {
			NamedParameterUtils.buildValueArray("xxx :a :b ?", paramMap);
			fail("mixed named parameters and ? placeholders not detected");
		}
		catch (InvalidDataAccessApiUsageException expected) {
		}
	}

	@Test
	public void convertTypeMapToArray() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a", 1).addValue("b", "b", 2).addValue("c", "c", 3);
		assertSame(3, NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams).length);
		assertSame(5, NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams).length);
		assertSame(5, NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :a :a xx :a :a"), namedParams).length);
		assertEquals(2, NamedParameterUtils
				.buildSqlTypeArray(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams)[4]);
	}

	@Test
	public void convertTypeMapToSqlParameterList() {
		MapSqlParameterSource namedParams = new MapSqlParameterSource();
		namedParams.addValue("a", "a", 1).addValue("b", "b", 2).addValue("c", "c", 3, "SQL_TYPE");
		assertSame(3, NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams).size());
		assertSame(5, NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams).size());
		assertSame(5, NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :a :a xx :a :a"), namedParams).size());
		assertEquals(2, NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c xx :a :b"), namedParams).get(4).getSqlType());
		assertEquals("SQL_TYPE", NamedParameterUtils
				.buildSqlParameterList(NamedParameterUtils.parseSqlStatement("xxx :a :b :c"), namedParams).get(2).getTypeName());
	}

	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void buildValueArrayWithMissingParameterValue() {
		String sql = "select count(0) from foo where id = :id";
		NamedParameterUtils.buildValueArray(sql, Collections.<String, Object>emptyMap());
	}

	@Test
	public void substituteNamedParametersWithStringContainingQuotes() {
		String expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'";
		String sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'";
		String newSql = NamedParameterUtils.substituteNamedParameters(sql, new MapSqlParameterSource());
		assertEquals(expectedSql, newSql);
	}

	@Test
	public void testParseSqlStatementWithStringContainingQuotes() {
		String expectedSql = "select 'first name' from artists where id = ? and quote = 'exsqueeze me?'";
		String sql = "select 'first name' from artists where id = :id and quote = 'exsqueeze me?'";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(expectedSql, NamedParameterUtils.substituteNamedParameters(parsedSql, null));
	}

	@Test  // SPR-4789
	public void parseSqlContainingComments() {
		String sql1 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX\n";
		ParsedSql psql1 = NamedParameterUtils.parseSqlStatement(sql1);
		assertEquals("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX\n",
				NamedParameterUtils.substituteNamedParameters(psql1, null));
		MapSqlParameterSource paramMap = new MapSqlParameterSource();
		paramMap.addValue("a", "a");
		paramMap.addValue("b", "b");
		paramMap.addValue("c", "c");
		Object[] params = NamedParameterUtils.buildValueArray(psql1, paramMap, null);
		assertEquals(4, params.length);
		assertEquals("a", params[0]);
		assertEquals("b", params[1]);
		assertEquals("c", params[2]);
		assertEquals("a", params[3]);

		String sql2 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz -- :xx XX";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertEquals("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz -- :xx XX",
				NamedParameterUtils.substituteNamedParameters(psql2, null));

		String sql3 = "/*+ HINT */ xxx /* comment ? */ :a yyyy :b :c :a zzzzz /* :xx XX*";
		ParsedSql psql3 = NamedParameterUtils.parseSqlStatement(sql3);
		assertEquals("/*+ HINT */ xxx /* comment ? */ ? yyyy ? ? ? zzzzz /* :xx XX*",
				NamedParameterUtils.substituteNamedParameters(psql3, null));

		String sql4 = "/*+ HINT */ xxx /* comment :a ? */ :a yyyy :b :c :a zzzzz /* :xx XX*";
		ParsedSql psql4 = NamedParameterUtils.parseSqlStatement(sql4);
		Map<String, String> parameters = Collections.singletonMap("a", "0");
		assertEquals("/*+ HINT */ xxx /* comment :a ? */ ? yyyy ? ? ? zzzzz /* :xx XX*",
				NamedParameterUtils.substituteNamedParameters(psql4, new MapSqlParameterSource(parameters)));
	}

	@Test  // SPR-4612
	public void parseSqlStatementWithPostgresCasting() {
		String expectedSql = "select 'first name' from artists where id = ? and birth_date=?::timestamp";
		String sql = "select 'first name' from artists where id = :id and birth_date=:birthDate::timestamp";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(expectedSql, NamedParameterUtils.substituteNamedParameters(parsedSql, null));
	}

	@Test  // SPR-13582
	public void parseSqlStatementWithPostgresContainedOperator() {
		String expectedSql = "select 'first name' from artists where info->'stat'->'albums' = ?? ? and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		String sql = "select 'first name' from artists where info->'stat'->'albums' = ?? :album and '[\"1\",\"2\",\"3\"]'::jsonb ?? '4'";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(1, parsedSql.getTotalParameterCount());
		assertEquals(expectedSql, NamedParameterUtils.substituteNamedParameters(parsedSql, null));
	}

	@Test  // SPR-15382
	public void parseSqlStatementWithPostgresAnyArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?| '{1,3,11,12,17}'::text[]";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(0, parsedSql.getTotalParameterCount());
		assertEquals(expectedSql, NamedParameterUtils.substituteNamedParameters(parsedSql, null));
	}

	@Test  // SPR-15382
	public void parseSqlStatementWithPostgresAllArrayStringsExistsOperator() {
		String expectedSql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND ? = 'Back in Black'";
		String sql = "select '[\"3\", \"11\"]'::jsonb ?& '{1,3,11,12,17}'::text[] AND :album = 'Back in Black'";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(1, parsedSql.getTotalParameterCount());
		assertEquals(expectedSql, NamedParameterUtils.substituteNamedParameters(parsedSql, null));
	}

	@Test  // SPR-7476
	public void parseSqlStatementWithEscapedColon() {
		String expectedSql = "select '0\\:0' as a, foo from bar where baz < DATE(? 23:59:59) and baz = ?";
		String sql = "select '0\\:0' as a, foo from bar where baz < DATE(:p1 23\\:59\\:59) and baz = :p2";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(2, parsedSql.getParameterNames().size());
		assertEquals("p1", parsedSql.getParameterNames().get(0));
		assertEquals("p2", parsedSql.getParameterNames().get(1));
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertEquals(expectedSql, finalSql);
	}

	@Test  // SPR-7476
	public void parseSqlStatementWithBracketDelimitedParameterNames() {
		String expectedSql = "select foo from bar where baz = b??z";
		String sql = "select foo from bar where baz = b:{p1}:{p2}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(2, parsedSql.getParameterNames().size());
		assertEquals("p1", parsedSql.getParameterNames().get(0));
		assertEquals("p2", parsedSql.getParameterNames().get(1));
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertEquals(expectedSql, finalSql);
	}

	@Test  // SPR-7476
	public void parseSqlStatementWithEmptyBracketsOrBracketsInQuotes() {
		String expectedSql = "select foo from bar where baz = b:{}z";
		String sql = "select foo from bar where baz = b:{}z";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(0, parsedSql.getParameterNames().size());
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertEquals(expectedSql, finalSql);

		String expectedSql2 = "select foo from bar where baz = 'b:{p1}z'";
		String sql2 = "select foo from bar where baz = 'b:{p1}z'";

		ParsedSql parsedSql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertEquals(0, parsedSql2.getParameterNames().size());
		String finalSql2 = NamedParameterUtils.substituteNamedParameters(parsedSql2, null);
		assertEquals(expectedSql2, finalSql2);
	}

	@Test
	public void parseSqlStatementWithSingleLetterInBrackets() {
		String expectedSql = "select foo from bar where baz = b?z";
		String sql = "select foo from bar where baz = b:{p}z";

		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(1, parsedSql.getParameterNames().size());
		assertEquals("p", parsedSql.getParameterNames().get(0));
		String finalSql = NamedParameterUtils.substituteNamedParameters(parsedSql, null);
		assertEquals(expectedSql, finalSql);
	}

	@Test  // SPR-2544
	public void parseSqlStatementWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";
		ParsedSql parsedSql = NamedParameterUtils.parseSqlStatement(expectedSql);
		assertEquals(expectedSql, NamedParameterUtils.substituteNamedParameters(parsedSql, null));
	}

	@Test  // SPR-2544
	public void substituteNamedParametersWithLogicalAnd() {
		String expectedSql = "xxx & yyyy";
		String newSql = NamedParameterUtils.substituteNamedParameters(expectedSql, new MapSqlParameterSource());
		assertEquals(expectedSql, newSql);
	}

	@Test  // SPR-3173
	public void variableAssignmentOperator() {
		String expectedSql = "x := 1";
		String newSql = NamedParameterUtils.substituteNamedParameters(expectedSql, new MapSqlParameterSource());
		assertEquals(expectedSql, newSql);
	}

	@Test  // SPR-8280
	public void parseSqlStatementWithQuotedSingleQuote() {
		String sql = "SELECT ':foo'':doo', :xxx FROM DUAL";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(1, psql.getTotalParameterCount());
		assertEquals("xxx", psql.getParameterNames().get(0));
	}

	@Test
	public void parseSqlStatementWithQuotesAndCommentBefore() {
		String sql = "SELECT /*:doo*/':foo', :xxx FROM DUAL";
		ParsedSql psql = NamedParameterUtils.parseSqlStatement(sql);
		assertEquals(1, psql.getTotalParameterCount());
		assertEquals("xxx", psql.getParameterNames().get(0));
	}

	@Test
	public void parseSqlStatementWithQuotesAndCommentAfter() {
		String sql2 = "SELECT ':foo'/*:doo*/, :xxx FROM DUAL";
		ParsedSql psql2 = NamedParameterUtils.parseSqlStatement(sql2);
		assertEquals(1, psql2.getTotalParameterCount());
		assertEquals("xxx", psql2.getParameterNames().get(0));
	}

}
