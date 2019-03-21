/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.test.context.jdbc;

import java.lang.reflect.Method;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.springframework.jdbc.datasource.init.ScriptUtils.*;
import static org.springframework.test.context.jdbc.SqlConfig.ErrorMode.*;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.*;

/**
 * Unit tests for {@link MergedSqlConfig}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class MergedSqlConfigTests {

	private void assertDefaults(MergedSqlConfig cfg) {
		assertNotNull(cfg);
		assertEquals("dataSource", "", cfg.getDataSource());
		assertEquals("transactionManager", "", cfg.getTransactionManager());
		assertEquals("transactionMode", INFERRED, cfg.getTransactionMode());
		assertEquals("encoding", "", cfg.getEncoding());
		assertEquals("separator", DEFAULT_STATEMENT_SEPARATOR, cfg.getSeparator());
		assertEquals("commentPrefix", DEFAULT_COMMENT_PREFIX, cfg.getCommentPrefix());
		assertEquals("blockCommentStartDelimiter", DEFAULT_BLOCK_COMMENT_START_DELIMITER,
			cfg.getBlockCommentStartDelimiter());
		assertEquals("blockCommentEndDelimiter", DEFAULT_BLOCK_COMMENT_END_DELIMITER, cfg.getBlockCommentEndDelimiter());
		assertEquals("errorMode", FAIL_ON_ERROR, cfg.getErrorMode());
	}

	@Test
	public void localConfigWithDefaults() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithDefaults");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());
		assertDefaults(cfg);
	}

	@Test
	public void globalConfigWithDefaults() throws Exception {
		Method method = GlobalConfigWithDefaultsClass.class.getMethod("globalConfigMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigWithDefaultsClass.class);
		assertDefaults(cfg);
	}

	@Test
	public void localConfigWithCustomValues() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithCustomValues");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());
		assertNotNull(cfg);
		assertEquals("dataSource", "ds", cfg.getDataSource());
		assertEquals("transactionManager", "txMgr", cfg.getTransactionManager());
		assertEquals("transactionMode", ISOLATED, cfg.getTransactionMode());
		assertEquals("encoding", "enigma", cfg.getEncoding());
		assertEquals("separator", "\n", cfg.getSeparator());
		assertEquals("commentPrefix", "`", cfg.getCommentPrefix());
		assertEquals("blockCommentStartDelimiter", "<<", cfg.getBlockCommentStartDelimiter());
		assertEquals("blockCommentEndDelimiter", ">>", cfg.getBlockCommentEndDelimiter());
		assertEquals("errorMode", IGNORE_FAILED_DROPS, cfg.getErrorMode());
	}

	@Test
	public void localConfigWithContinueOnError() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithContinueOnError");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());
		assertNotNull(cfg);
		assertEquals("errorMode", CONTINUE_ON_ERROR, cfg.getErrorMode());
	}

	@Test
	public void localConfigWithIgnoreFailedDrops() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithIgnoreFailedDrops");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());
		assertNotNull(cfg);
		assertEquals("errorMode", IGNORE_FAILED_DROPS, cfg.getErrorMode());
	}

	@Test
	public void globalConfig() throws Exception {
		Method method = GlobalConfigClass.class.getMethod("globalConfigMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigClass.class);
		assertNotNull(cfg);
		assertEquals("dataSource", "", cfg.getDataSource());
		assertEquals("transactionManager", "", cfg.getTransactionManager());
		assertEquals("transactionMode", INFERRED, cfg.getTransactionMode());
		assertEquals("encoding", "global", cfg.getEncoding());
		assertEquals("separator", "\n", cfg.getSeparator());
		assertEquals("commentPrefix", DEFAULT_COMMENT_PREFIX, cfg.getCommentPrefix());
		assertEquals("blockCommentStartDelimiter", DEFAULT_BLOCK_COMMENT_START_DELIMITER,
			cfg.getBlockCommentStartDelimiter());
		assertEquals("blockCommentEndDelimiter", DEFAULT_BLOCK_COMMENT_END_DELIMITER, cfg.getBlockCommentEndDelimiter());
		assertEquals("errorMode", IGNORE_FAILED_DROPS, cfg.getErrorMode());
	}

	@Test
	public void globalConfigWithLocalOverrides() throws Exception {
		Method method = GlobalConfigClass.class.getMethod("globalConfigWithLocalOverridesMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigClass.class);

		assertNotNull(cfg);
		assertEquals("dataSource", "", cfg.getDataSource());
		assertEquals("transactionManager", "", cfg.getTransactionManager());
		assertEquals("transactionMode", INFERRED, cfg.getTransactionMode());
		assertEquals("encoding", "local", cfg.getEncoding());
		assertEquals("separator", "@@", cfg.getSeparator());
		assertEquals("commentPrefix", DEFAULT_COMMENT_PREFIX, cfg.getCommentPrefix());
		assertEquals("blockCommentStartDelimiter", DEFAULT_BLOCK_COMMENT_START_DELIMITER,
			cfg.getBlockCommentStartDelimiter());
		assertEquals("blockCommentEndDelimiter", DEFAULT_BLOCK_COMMENT_END_DELIMITER, cfg.getBlockCommentEndDelimiter());
		assertEquals("errorMode", CONTINUE_ON_ERROR, cfg.getErrorMode());
	}

	// -------------------------------------------------------------------------

	@Sql
	public static void localConfigMethodWithDefaults() {
	}

	@Sql(config = @SqlConfig(dataSource = "ds", transactionManager = "txMgr", transactionMode = ISOLATED, encoding = "enigma", separator = "\n", commentPrefix = "`", blockCommentStartDelimiter = "<<", blockCommentEndDelimiter = ">>", errorMode = IGNORE_FAILED_DROPS))
	public static void localConfigMethodWithCustomValues() {
	}

	@Sql(config = @SqlConfig(errorMode = CONTINUE_ON_ERROR))
	public static void localConfigMethodWithContinueOnError() {
	}

	@Sql(config = @SqlConfig(errorMode = IGNORE_FAILED_DROPS))
	public static void localConfigMethodWithIgnoreFailedDrops() {
	}


	@SqlConfig
	public static class GlobalConfigWithDefaultsClass {

		@Sql("foo.sql")
		public void globalConfigMethod() {
		}
	}

	@SqlConfig(encoding = "global", separator = "\n", errorMode = IGNORE_FAILED_DROPS)
	public static class GlobalConfigClass {

		@Sql("foo.sql")
		public void globalConfigMethod() {
		}

		@Sql(scripts = "foo.sql", config = @SqlConfig(encoding = "local", separator = "@@", errorMode = CONTINUE_ON_ERROR))
		public void globalConfigWithLocalOverridesMethod() {
		}
	}

}
