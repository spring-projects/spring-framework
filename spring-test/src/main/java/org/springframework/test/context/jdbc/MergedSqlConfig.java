/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 * attributes declared locally via {@link Sql#config} and globally as a
 * class-level annotation.
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


	private static TransactionMode retrieveTransactionMode(AnnotationAttributes attributes) {
		TransactionMode transactionMode = attributes.getEnum("transactionMode");
		if (transactionMode == TransactionMode.DEFAULT) {
			transactionMode = TransactionMode.INFERRED;
		}
		return transactionMode;
	}

	private static ErrorMode retrieveErrorMode(AnnotationAttributes attributes) {
		ErrorMode errorMode = attributes.getEnum("errorMode");
		if (errorMode == ErrorMode.DEFAULT) {
			errorMode = ErrorMode.FAIL_ON_ERROR;
		}
		return errorMode;
	}

	private static String retrieveSeparator(AnnotationAttributes attributes) {
		String separator = attributes.getString("separator");
		if ("".equals(separator)) {
			separator = ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;
		}
		return separator;
	}

	private static String retrieveCommentPrefix(AnnotationAttributes attributes) {
		String commentPrefix = attributes.getString("commentPrefix");
		if ("".equals(commentPrefix)) {
			commentPrefix = ScriptUtils.DEFAULT_COMMENT_PREFIX;
		}
		return commentPrefix;
	}

	private static String retrieveBlockCommentStartDelimiter(AnnotationAttributes attributes) {
		String blockCommentStartDelimiter = attributes.getString("blockCommentStartDelimiter");
		if ("".equals(blockCommentStartDelimiter)) {
			blockCommentStartDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;
		}
		return blockCommentStartDelimiter;
	}

	private static String retrieveBlockCommentEndDelimiter(AnnotationAttributes attributes) {
		String blockCommentEndDelimiter = attributes.getString("blockCommentEndDelimiter");
		if ("".equals(blockCommentEndDelimiter)) {
			blockCommentEndDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;
		}
		return blockCommentEndDelimiter;
	}

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
		AnnotationAttributes attributes = AnnotatedElementUtils.getAnnotationAttributes(testClass,
			SqlConfig.class.getName());

		// Override global attributes with local attributes.
		if (attributes != null) {
			for (String key : attributes.keySet()) {
				Object value = AnnotationUtils.getValue(localSqlConfig, key);
				if (value != null) {
					// Is the value explicit (i.e., not a 'default')?
					if (!value.equals("") && (value != TransactionMode.DEFAULT) && (value != ErrorMode.DEFAULT)) {
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
		this.transactionMode = retrieveTransactionMode(attributes);
		this.encoding = attributes.getString("encoding");
		this.separator = retrieveSeparator(attributes);
		this.commentPrefix = retrieveCommentPrefix(attributes);
		this.blockCommentStartDelimiter = retrieveBlockCommentStartDelimiter(attributes);
		this.blockCommentEndDelimiter = retrieveBlockCommentEndDelimiter(attributes);
		this.errorMode = retrieveErrorMode(attributes);
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
		return new ToStringCreator(this)//
		.append("dataSource", dataSource)//
		.append("transactionManager", transactionManager)//
		.append("transactionMode", transactionMode)//
		.append("encoding", encoding)//
		.append("separator", separator)//
		.append("commentPrefix", commentPrefix)//
		.append("blockCommentStartDelimiter", blockCommentStartDelimiter)//
		.append("blockCommentEndDelimiter", blockCommentEndDelimiter)//
		.append("errorMode", errorMode)//
		.toString();
	}

}
