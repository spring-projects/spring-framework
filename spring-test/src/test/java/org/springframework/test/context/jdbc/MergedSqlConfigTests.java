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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;
import static org.springframework.jdbc.datasource.init.ScriptUtils.DEFAULT_COMMENT_PREFIXES;
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
class MergedSqlConfigTests {

	@Test
	void nullLocalSqlConfig() {
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(null, getClass()))
			.withMessage("Local @SqlConfig must not be null");
	}

	@Test
	void nullTestClass() {
		SqlConfig sqlConfig = GlobalConfigClass.class.getAnnotation(SqlConfig.class);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(sqlConfig, null))
			.withMessage("testClass must not be null");
	}

	@Test
	void localConfigWithEmptyCommentPrefix() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithEmptyCommentPrefix");
		SqlConfig sqlConfig = method.getAnnotation(Sql.class).config();

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(sqlConfig, getClass()))
			.withMessage("@SqlConfig(commentPrefix) must contain text");
	}

	@Test
	void localConfigWithEmptyCommentPrefixes() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithEmptyCommentPrefixes");
		SqlConfig sqlConfig = method.getAnnotation(Sql.class).config();

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(sqlConfig, getClass()))
			.withMessage("@SqlConfig(commentPrefixes) must not contain empty prefixes");
	}

	@Test
	void localConfigWithDuplicatedCommentPrefixes() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithDuplicatedCommentPrefixes");
		SqlConfig sqlConfig = method.getAnnotation(Sql.class).config();

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(sqlConfig, getClass()))
			.withMessage("You may declare the 'commentPrefix' or 'commentPrefixes' attribute in @SqlConfig but not both");
	}

	@Test
	void localConfigWithDefaults() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithDefaults");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());
		assertDefaults(cfg);
	}

	@Test
	void localConfigWithCustomValues() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithCustomValues");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());

		assertSoftly(softly -> {
			softly.assertThat(cfg).isNotNull();
			softly.assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("ds");
			softly.assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("txMgr");
			softly.assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(ISOLATED);
			softly.assertThat(cfg.getEncoding()).as("encoding").isEqualTo("enigma");
			softly.assertThat(cfg.getSeparator()).as("separator").isEqualTo("\n");
			softly.assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("`"));
			softly.assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo("<<");
			softly.assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(">>");
			softly.assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(IGNORE_FAILED_DROPS);
		});
	}

	@Test
	void localConfigWithCustomCommentPrefixes() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithCustomCommentPrefixes");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());

		assertThat(cfg).isNotNull();
		assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("`"));
	}

	@Test
	void localConfigWithMultipleCommentPrefixes() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithMultipleCommentPrefixes");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());

		assertThat(cfg).isNotNull();
		assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("`", "--"));
	}

	@Test
	void localConfigWithContinueOnError() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithContinueOnError");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());

		assertThat(cfg).isNotNull();
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(CONTINUE_ON_ERROR);
	}

	@Test
	void localConfigWithIgnoreFailedDrops() throws Exception {
		Method method = getClass().getMethod("localConfigMethodWithIgnoreFailedDrops");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, getClass());

		assertThat(cfg).isNotNull();
		assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(IGNORE_FAILED_DROPS);
	}

	@Test
	void globalConfigWithEmptyCommentPrefix() throws Exception {
		SqlConfig sqlConfig = GlobalConfigWithWithEmptyCommentPrefixClass.class.getAnnotation(SqlConfig.class);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(sqlConfig, getClass()))
			.withMessage("@SqlConfig(commentPrefix) must contain text");
	}

	@Test
	void globalConfigWithEmptyCommentPrefixes() throws Exception {
		SqlConfig sqlConfig = GlobalConfigWithWithEmptyCommentPrefixesClass.class.getAnnotation(SqlConfig.class);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(sqlConfig, getClass()))
			.withMessage("@SqlConfig(commentPrefixes) must not contain empty prefixes");
	}

	@Test
	void globalConfigWithDuplicatedCommentPrefixes() throws Exception {
		SqlConfig sqlConfig = GlobalConfigWithWithDuplicatedCommentPrefixesClass.class.getAnnotation(SqlConfig.class);

		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MergedSqlConfig(sqlConfig, getClass()))
			.withMessage("You may declare the 'commentPrefix' or 'commentPrefixes' attribute in @SqlConfig but not both");
	}

	@Test
	void globalConfigWithDefaults() throws Exception {
		Method method = GlobalConfigWithDefaultsClass.class.getMethod("globalConfigMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigWithDefaultsClass.class);
		assertDefaults(cfg);
	}

	@Test
	void globalConfig() throws Exception {
		Method method = GlobalConfigClass.class.getMethod("globalConfigMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigClass.class);

		assertSoftly(softly -> {
			softly.assertThat(cfg).isNotNull();
			softly.assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("");
			softly.assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("");
			softly.assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(INFERRED);
			softly.assertThat(cfg.getEncoding()).as("encoding").isEqualTo("global");
			softly.assertThat(cfg.getSeparator()).as("separator").isEqualTo("\n");
			softly.assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("`", "--"));
			softly.assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_START_DELIMITER);
			softly.assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_END_DELIMITER);
			softly.assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(IGNORE_FAILED_DROPS);
		});
	}

	@Test
	void globalConfigWithLocalOverrides() throws Exception {
		Method method = GlobalConfigClass.class.getMethod("globalConfigWithLocalOverridesMethod");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, GlobalConfigClass.class);

		assertSoftly(softly -> {
			softly.assertThat(cfg).isNotNull();
			softly.assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("");
			softly.assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("");
			softly.assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(INFERRED);
			softly.assertThat(cfg.getEncoding()).as("encoding").isEqualTo("local");
			softly.assertThat(cfg.getSeparator()).as("separator").isEqualTo("@@");
			softly.assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("#"));
			softly.assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_START_DELIMITER);
			softly.assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_END_DELIMITER);
			softly.assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(CONTINUE_ON_ERROR);
		});
	}

	@Test
	void globalConfigWithCommentPrefixAndLocalOverrides() throws Exception {
		Class<?> testClass = GlobalConfigWithPrefixClass.class;

		Method method = testClass.getMethod("commentPrefixesOverrideCommentPrefix");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, testClass);

		assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("#", "@"));

		method = testClass.getMethod("commentPrefixOverridesCommentPrefix");
		localSqlConfig = method.getAnnotation(Sql.class).config();
		cfg = new MergedSqlConfig(localSqlConfig, testClass);

		assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("#"));
	}

	@Test
	void globalConfigWithCommentPrefixesAndLocalOverrides() throws Exception {
		Class<?> testClass = GlobalConfigWithPrefixesClass.class;

		Method method = testClass.getMethod("commentPrefixesOverrideCommentPrefixes");
		SqlConfig localSqlConfig = method.getAnnotation(Sql.class).config();
		MergedSqlConfig cfg = new MergedSqlConfig(localSqlConfig, testClass);

		assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("#", "@"));

		method = testClass.getMethod("commentPrefixOverridesCommentPrefixes");
		localSqlConfig = method.getAnnotation(Sql.class).config();
		cfg = new MergedSqlConfig(localSqlConfig, testClass);

		assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(array("#"));
	}

	private void assertDefaults(MergedSqlConfig cfg) {
		assertSoftly(softly -> {
			softly.assertThat(cfg).isNotNull();
			softly.assertThat(cfg.getDataSource()).as("dataSource").isEqualTo("");
			softly.assertThat(cfg.getTransactionManager()).as("transactionManager").isEqualTo("");
			softly.assertThat(cfg.getTransactionMode()).as("transactionMode").isEqualTo(INFERRED);
			softly.assertThat(cfg.getEncoding()).as("encoding").isEqualTo("");
			softly.assertThat(cfg.getSeparator()).as("separator").isEqualTo(DEFAULT_STATEMENT_SEPARATOR);
			softly.assertThat(cfg.getCommentPrefixes()).as("commentPrefixes").isEqualTo(DEFAULT_COMMENT_PREFIXES);
			softly.assertThat(cfg.getBlockCommentStartDelimiter()).as("blockCommentStartDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_START_DELIMITER);
			softly.assertThat(cfg.getBlockCommentEndDelimiter()).as("blockCommentEndDelimiter").isEqualTo(DEFAULT_BLOCK_COMMENT_END_DELIMITER);
			softly.assertThat(cfg.getErrorMode()).as("errorMode").isEqualTo(FAIL_ON_ERROR);
		});
	}

	private static String[] array(String... elements) {
		return elements;
	}

	// -------------------------------------------------------------------------

	@Sql(config = @SqlConfig(commentPrefix = "#", commentPrefixes = "#" ))
	public static void localConfigMethodWithDuplicatedCommentPrefixes() {
	}

	@Sql
	public static void localConfigMethodWithDefaults() {
	}

	@Sql(config = @SqlConfig(dataSource = "ds", transactionManager = "txMgr", transactionMode = ISOLATED, encoding = "enigma", separator = "\n", commentPrefix = "`", blockCommentStartDelimiter = "<<", blockCommentEndDelimiter = ">>", errorMode = IGNORE_FAILED_DROPS))
	public static void localConfigMethodWithCustomValues() {
	}

	@Sql(config = @SqlConfig(commentPrefix = "   " ))
	public static void localConfigMethodWithEmptyCommentPrefix() {
	}

	@Sql(config = @SqlConfig(commentPrefixes = { "--", "   " }))
	public static void localConfigMethodWithEmptyCommentPrefixes() {
	}

	@Sql(config = @SqlConfig(commentPrefixes = "`"))
	public static void localConfigMethodWithCustomCommentPrefixes() {
	}

	@Sql(config = @SqlConfig(commentPrefixes = { "`", "--" }))
	public static void localConfigMethodWithMultipleCommentPrefixes() {
	}

	@Sql(config = @SqlConfig(errorMode = CONTINUE_ON_ERROR))
	public static void localConfigMethodWithContinueOnError() {
	}

	@Sql(config = @SqlConfig(errorMode = IGNORE_FAILED_DROPS))
	public static void localConfigMethodWithIgnoreFailedDrops() {
	}

	@SqlConfig(commentPrefix = "   ")
	public static class GlobalConfigWithWithEmptyCommentPrefixClass {
	}

	@SqlConfig(commentPrefixes = { "--", "   " })
	public static class GlobalConfigWithWithEmptyCommentPrefixesClass {
	}

	@SqlConfig(commentPrefix = "#", commentPrefixes = "#")
	public static class GlobalConfigWithWithDuplicatedCommentPrefixesClass {
	}

	@SqlConfig
	public static class GlobalConfigWithDefaultsClass {

		@Sql
		public void globalConfigMethod() {
		}
	}

	@SqlConfig(encoding = "global", separator = "\n", commentPrefixes = { "`", "--" }, errorMode = IGNORE_FAILED_DROPS)
	public static class GlobalConfigClass {

		@Sql
		public void globalConfigMethod() {
		}

		@Sql(config = @SqlConfig(encoding = "local", separator = "@@", commentPrefix = "#", errorMode = CONTINUE_ON_ERROR))
		public void globalConfigWithLocalOverridesMethod() {
		}
	}

	@SqlConfig(commentPrefix = "`")
	public static class GlobalConfigWithPrefixClass {

		@Sql(config = @SqlConfig(commentPrefixes = { "#", "@" }))
		public void commentPrefixesOverrideCommentPrefix() {
		}

		@Sql(config = @SqlConfig(commentPrefix = "#"))
		public void commentPrefixOverridesCommentPrefix() {
		}
	}

	@SqlConfig(commentPrefixes = { "`", "--" })
	public static class GlobalConfigWithPrefixesClass {

		@Sql(config = @SqlConfig(commentPrefixes = { "#", "@" }))
		public void commentPrefixesOverrideCommentPrefixes() {
		}

		@Sql(config = @SqlConfig(commentPrefix = "#"))
		public void commentPrefixOverridesCommentPrefixes() {
		}
	}

}
