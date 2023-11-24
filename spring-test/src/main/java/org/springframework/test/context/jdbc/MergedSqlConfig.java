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

package org.springframework.test.context.jdbc;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.style.DefaultToStringStyler;
import org.springframework.core.style.SimpleValueStyler;
import org.springframework.core.style.ToStringCreator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.lang.Nullable;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.jdbc.SqlConfig.ErrorMode;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;
import org.springframework.util.Assert;

/**
 * {@code MergedSqlConfig} encapsulates the <em>merged</em> {@link SqlConfig @SqlConfig}
 * attributes declared locally via {@link Sql#config} and globally as a class-level annotation.
 *
 * <p>Explicit local configuration attributes override global configuration attributes.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see SqlConfig
 */
class MergedSqlConfig {

	private static final String COMMENT_PREFIX = "commentPrefix";

	private static final String COMMENT_PREFIXES = "commentPrefixes";


	private final String dataSource;

	private final String transactionManager;

	private final TransactionMode transactionMode;

	private final String encoding;

	private final String separator;

	private final String[] commentPrefixes;

	private final String blockCommentStartDelimiter;

	private final String blockCommentEndDelimiter;

	private final ErrorMode errorMode;


	/**
	 * Construct a {@code MergedSqlConfig} instance by merging the configuration
	 * from the supplied local (potentially method-level) {@code @SqlConfig} annotation
	 * with class-level configuration discovered on the supplied {@code testClass}.
	 * <p>Local configuration overrides class-level configuration.
	 * <p>If the test class is not annotated with {@code @SqlConfig}, no merging
	 * takes place and the local configuration is used "as is".
	 */
	MergedSqlConfig(SqlConfig localSqlConfig, Class<?> testClass) {
		Assert.notNull(localSqlConfig, "Local @SqlConfig must not be null");
		Assert.notNull(testClass, "testClass must not be null");

		AnnotationAttributes mergedAttributes = mergeAttributes(localSqlConfig, testClass);

		this.dataSource = mergedAttributes.getString("dataSource");
		this.transactionManager = mergedAttributes.getString("transactionManager");
		this.transactionMode = getEnum(mergedAttributes, "transactionMode", TransactionMode.DEFAULT,
				TransactionMode.INFERRED);
		this.encoding = mergedAttributes.getString("encoding");
		this.separator = getString(mergedAttributes, "separator", ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
		this.commentPrefixes = getCommentPrefixes(mergedAttributes);
		this.blockCommentStartDelimiter = getString(mergedAttributes, "blockCommentStartDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER);
		this.blockCommentEndDelimiter = getString(mergedAttributes, "blockCommentEndDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		this.errorMode = getEnum(mergedAttributes, "errorMode", ErrorMode.DEFAULT, ErrorMode.FAIL_ON_ERROR);
	}

	private AnnotationAttributes mergeAttributes(SqlConfig localSqlConfig, Class<?> testClass) {
		AnnotationAttributes localAttributes = AnnotationUtils.getAnnotationAttributes(localSqlConfig, false, false);

		// Enforce comment prefix aliases within the local @SqlConfig.
		enforceCommentPrefixAliases(localAttributes);

		// Get global attributes, if any.
		SqlConfig globalSqlConfig = TestContextAnnotationUtils.findMergedAnnotation(testClass, SqlConfig.class);

		// Use local attributes only?
		if (globalSqlConfig == null) {
			return localAttributes;
		}

		AnnotationAttributes globalAttributes = AnnotationUtils.getAnnotationAttributes(globalSqlConfig, false, false);

		// Enforce comment prefix aliases within the global @SqlConfig.
		enforceCommentPrefixAliases(globalAttributes);

		for (String key : globalAttributes.keySet()) {
			Object value = localAttributes.get(key);
			if (isExplicitValue(value)) {
				// Override global attribute with local attribute.
				globalAttributes.put(key, value);

				// Ensure comment prefix aliases are honored during the merge.
				if (key.equals(COMMENT_PREFIX) && isEmptyArray(localAttributes.get(COMMENT_PREFIXES))) {
					globalAttributes.put(COMMENT_PREFIXES, value);
				}
				else if (key.equals(COMMENT_PREFIXES) && isEmptyString(localAttributes.get(COMMENT_PREFIX))) {
					globalAttributes.put(COMMENT_PREFIX, value);
				}
			}
		}
		return globalAttributes;
	}

	/**
	 * Get the bean name of the {@link javax.sql.DataSource}.
	 * @see SqlConfig#dataSource()
	 */
	String getDataSource() {
		return this.dataSource;
	}

	/**
	 * Get the bean name of the {@link org.springframework.transaction.PlatformTransactionManager}.
	 * @see SqlConfig#transactionManager()
	 */
	String getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * Get the {@link TransactionMode}.
	 * @see SqlConfig#transactionMode()
	 */
	TransactionMode getTransactionMode() {
		return this.transactionMode;
	}

	/**
	 * Get the encoding for the SQL scripts, if different from the platform
	 * encoding.
	 * @see SqlConfig#encoding()
	 */
	String getEncoding() {
		return this.encoding;
	}

	/**
	 * Get the character string used to separate individual statements within the
	 * SQL scripts.
	 * @see SqlConfig#separator()
	 */
	String getSeparator() {
		return this.separator;
	}

	/**
	 * Get the prefixes that identify single-line comments within the SQL scripts.
	 * @since 5.2
	 * @see SqlConfig#commentPrefixes()
	 */
	String[] getCommentPrefixes() {
		return this.commentPrefixes;
	}

	/**
	 * Get the start delimiter that identifies block comments within the SQL scripts.
	 * @see SqlConfig#blockCommentStartDelimiter()
	 */
	String getBlockCommentStartDelimiter() {
		return this.blockCommentStartDelimiter;
	}

	/**
	 * Get the end delimiter that identifies block comments within the SQL scripts.
	 * @see SqlConfig#blockCommentEndDelimiter()
	 */
	String getBlockCommentEndDelimiter() {
		return this.blockCommentEndDelimiter;
	}

	/**
	 * Get the {@link ErrorMode}.
	 * @see SqlConfig#errorMode()
	 */
	ErrorMode getErrorMode() {
		return this.errorMode;
	}

	/**
	 * Provide a String representation of the merged SQL script configuration.
	 */
	@Override
	public String toString() {
		return new ToStringCreator(this, new DefaultToStringStyler(new SimpleValueStyler()))
				.append("dataSource", this.dataSource)
				.append("transactionManager", this.transactionManager)
				.append("transactionMode", this.transactionMode)
				.append("encoding", this.encoding)
				.append("separator", this.separator)
				.append("commentPrefixes", this.commentPrefixes)
				.append("blockCommentStartDelimiter", this.blockCommentStartDelimiter)
				.append("blockCommentEndDelimiter", this.blockCommentEndDelimiter)
				.append("errorMode", this.errorMode)
				.toString();
	}


	private static <E extends Enum<?>> E getEnum(AnnotationAttributes attributes, String attributeName,
			E inheritedOrDefaultValue, E defaultValue) {

		E value = attributes.getEnum(attributeName);
		if (value == inheritedOrDefaultValue) {
			value = defaultValue;
		}
		return value;
	}

	private static String getString(AnnotationAttributes attributes, String attributeName, String defaultValue) {
		String value = attributes.getString(attributeName);
		if (value.isEmpty()) {
			value = defaultValue;
		}
		return value;
	}

	private static void enforceCommentPrefixAliases(AnnotationAttributes attributes) {
		String commentPrefix = attributes.getString(COMMENT_PREFIX);
		String[] commentPrefixes = attributes.getStringArray(COMMENT_PREFIXES);

		boolean explicitCommentPrefix = !commentPrefix.isEmpty();
		boolean explicitCommentPrefixes = (commentPrefixes.length != 0);
		Assert.isTrue(!(explicitCommentPrefix && explicitCommentPrefixes),
			"You may declare the 'commentPrefix' or 'commentPrefixes' attribute in @SqlConfig but not both");

		if (explicitCommentPrefix) {
			Assert.hasText(commentPrefix, "@SqlConfig(commentPrefix) must contain text");
			attributes.put(COMMENT_PREFIXES, new String[] { commentPrefix });
		}
		else if (explicitCommentPrefixes) {
			for (String prefix : commentPrefixes) {
				Assert.hasText(prefix, "@SqlConfig(commentPrefixes) must not contain empty prefixes");
			}
			attributes.put(COMMENT_PREFIX, commentPrefixes);
		}
		else {
			// We know commentPrefixes is an empty array, so make sure commentPrefix
			// is set to that as well in order to honor the alias contract.
			attributes.put(COMMENT_PREFIX, commentPrefixes);
		}
	}

	private static String[] getCommentPrefixes(AnnotationAttributes attributes) {
		String[] commentPrefix = attributes.getStringArray(COMMENT_PREFIX);
		String[] commentPrefixes = attributes.getStringArray(COMMENT_PREFIXES);

		Assert.state(Arrays.equals(commentPrefix, commentPrefixes),
			"Failed to properly handle 'commentPrefix' and 'commentPrefixes' aliases");

		return (commentPrefixes.length != 0 ? commentPrefixes : ScriptUtils.DEFAULT_COMMENT_PREFIXES);
	}

	/**
	 * Determine if the supplied value is an explicit value (i.e., not a default).
	 */
	private static boolean isExplicitValue(@Nullable Object value) {
		return !(isEmptyString(value) ||
				isEmptyArray(value) ||
				value == TransactionMode.DEFAULT ||
				value == ErrorMode.DEFAULT);
	}

	private static boolean isEmptyString(@Nullable Object value) {
		return (value instanceof String str && str.isEmpty());
	}

	private static boolean isEmptyArray(@Nullable Object value) {
		return (value != null && value.getClass().isArray() && Array.getLength(value) == 0);
	}

}
