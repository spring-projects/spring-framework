/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.style.ToStringCreator;
import org.springframework.jdbc.datasource.init.ScriptUtils;
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

	private final String dataSource;

	private final String transactionManager;

	private final TransactionMode transactionMode;

	private final String encoding;

	private final String separator;

	private final String commentPrefix;

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

		// Get global attributes, if any.
		AnnotationAttributes attributes = AnnotatedElementUtils.findMergedAnnotationAttributes(
				testClass, SqlConfig.class.getName(), false, false);

		// Override global attributes with local attributes.
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				Object value = AnnotationUtils.getValue(localSqlConfig, key);
				if (value != null) {
					// Is the value explicit (i.e., not a 'default')?
					if (!value.equals("") && value != TransactionMode.DEFAULT && value != ErrorMode.DEFAULT) {
						attributes.put(key, value);
					}
				}
			}
		}
		else {
			// Otherwise, use local attributes only.
			attributes = AnnotationUtils.getAnnotationAttributes(localSqlConfig, false, false);
		}

		this.dataSource = attributes.getString("dataSource");
		this.transactionManager = attributes.getString("transactionManager");
		this.transactionMode = getEnum(attributes, "transactionMode", TransactionMode.DEFAULT, TransactionMode.INFERRED);
		this.encoding = attributes.getString("encoding");
		this.separator = getString(attributes, "separator", ScriptUtils.DEFAULT_STATEMENT_SEPARATOR);
		this.commentPrefix = getString(attributes, "commentPrefix", ScriptUtils.DEFAULT_COMMENT_PREFIX);
		this.blockCommentStartDelimiter = getString(attributes, "blockCommentStartDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER);
		this.blockCommentEndDelimiter = getString(attributes, "blockCommentEndDelimiter",
				ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
		this.errorMode = getEnum(attributes, "errorMode", ErrorMode.DEFAULT, ErrorMode.FAIL_ON_ERROR);
	}

	/**
	 * @see SqlConfig#dataSource()
	 */
	String getDataSource() {
		return this.dataSource;
	}

	/**
	 * @see SqlConfig#transactionManager()
	 */
	String getTransactionManager() {
		return this.transactionManager;
	}

	/**
	 * @see SqlConfig#transactionMode()
	 */
	TransactionMode getTransactionMode() {
		return this.transactionMode;
	}

	/**
	 * @see SqlConfig#encoding()
	 */
	String getEncoding() {
		return this.encoding;
	}

	/**
	 * @see SqlConfig#separator()
	 */
	String getSeparator() {
		return this.separator;
	}

	/**
	 * @see SqlConfig#commentPrefix()
	 */
	String getCommentPrefix() {
		return this.commentPrefix;
	}

	/**
	 * @see SqlConfig#blockCommentStartDelimiter()
	 */
	String getBlockCommentStartDelimiter() {
		return this.blockCommentStartDelimiter;
	}

	/**
	 * @see SqlConfig#blockCommentEndDelimiter()
	 */
	String getBlockCommentEndDelimiter() {
		return this.blockCommentEndDelimiter;
	}

	/**
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
		return new ToStringCreator(this)
				.append("dataSource", this.dataSource)
				.append("transactionManager", this.transactionManager)
				.append("transactionMode", this.transactionMode)
				.append("encoding", this.encoding)
				.append("separator", this.separator)
				.append("commentPrefix", this.commentPrefix)
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
		if ("".equals(value)) {
			value = defaultValue;
		}
		return value;
	}

}
