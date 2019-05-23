/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_COMMENT_PREFIX;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;
import static org.springframework.test.context.jdbc.SqlConfig.ErrorMode.CONTINUE_ON_ERROR;
import static org.springframework.test.context.jdbc.SqlConfig.ErrorMode.FAIL_ON_ERROR;
import static org.springframework.test.context.jdbc.SqlConfig.ErrorMode.IGNORE_FAILED_DROPS;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.INFERRED;
import static org.springframework.test.context.jdbc.SqlConfig.TransactionMode.ISOLATED;

/**
 * Unit tests for {@link MergedSqlConfig}.
 *
 * @author Sam Brannen
 * @since 4.1
 */
public class MergedSqlConfigTests {

	private void assertDefaults(MergedSqlConfig cfg) {
		assertThat(cfg).isNotNull();
		assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("");
		assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("");
		assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(INFERRED);
		assertThat(cfg.getEncoding()).as("encoding").isEqualTo("");
		assertThat(cfg.getSeparator()).as("separator").isEqualTo(DEFAULT_STATEMENT_SEPARATOR);
		assertThat(cfg.getCommentPrefix()).as("commentPrefix").isEqualTo(DEFAULT_COMMENT_PREFIX);
		assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_START_DELIMITER);
		assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(FAIL_ON_ERROR);
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
		assertThat(cfg).isNotNull();
		assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("ds");
		assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("txMgr");
		assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(ISOLATED);
		assertThat(cfg.getEncoding()).as("encoding").isEqualTo("enigma");
		assertThat(cfg.getSeparator()).as("separator").isEqualTo("\n");
		assertThat(cfg.getCommentPrefix()).as("commentPrefix").isEqualTo("`");
		assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo("<<");
		assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(">>");
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(IGNORE_FAILED_DROPS);
	}

	@Test
	public void localConfigWithContinueOnError() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithContinueOnError");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());
		assertThat(cfg).isNotNull();
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(CONTINUE_ON_ERROR);
	}

	@Test
	public void localConfigWithIgnoreFailedDrops() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithIgnoreFailedDrops");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());
		assertThat(cfg).isNotNull();
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(IGNORE_FAILED_DROPS);
	}

	@Test
	public void globalConfig() throws Exception {
		Method method = GlobalConfigClass.class.getMethod("globalConfigMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigClass.class);
		assertThat(cfg).isNotNull();
		assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("");
		assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("");
		assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(INFERRED);
		assertThat(cfg.getEncoding()).as("encoding").isEqualTo("global");
		assertThat(cfg.getSeparator()).as("separator").isEqualTo("\n");
		assertThat(cfg.getCommentPrefix()).as("commentPrefix").isEqualTo(DEFAULT_COMMENT_PREFIX);
		assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_START_DELIMITER);
		assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(IGNORE_FAILED_DROPS);
	}

	@Test
	public void globalConfigWithLocalOverrides() throws Exception {
		Method method = GlobalConfigClass.class.getMethod("globalConfigWithLocalOverridesMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigClass.class);

		assertThat(cfg).isNotNull();
		assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("");
		assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("");
		assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(INFERRED);
		assertThat(cfg.getEncoding()).as("encoding").isEqualTo("local");
		assertThat(cfg.getSeparator()).as("separator").isEqualTo("@@");
		assertThat(cfg.getCommentPrefix()).as("commentPrefix").isEqualTo(DEFAULT_COMMENT_PREFIX);
		assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_START_DELIMITER);
		assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(CONTINUE_ON_ERROR);
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
