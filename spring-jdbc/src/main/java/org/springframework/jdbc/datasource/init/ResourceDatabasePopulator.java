/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.jdbc.datasource.init;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Populates, initializes, or cleans up a database using SQL scripts defined in
 * external resources.
 *
 * <ul>
 * <li>Call {@link #addScript} to add a single SQL script location.
 * <li>Call {@link #addScripts} to add multiple SQL script locations.
 * <li>Consult the setter methods in this class for further configuration options.
 * <li>Call {@link #populate} or {@link #execute} to initialize or clean up the
 * database using the configured scripts.
 * </ul>
 *
 * @author Keith Donald
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Oliver Gierke
 * @author Sam Brannen
 * @author Chris Baldwin
 * @since 3.0
 * @see DatabasePopulatorUtils
 * @see ScriptUtils
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

	private List<Resource> scripts = new ArrayList<Resource>();

	private String sqlScriptEncoding;

	private String separator = ScriptUtils.DEFAULT_STATEMENT_SEPARATOR;

	private String commentPrefix = ScriptUtils.DEFAULT_COMMENT_PREFIX;

	private String blockCommentStartDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER;

	private String blockCommentEndDelimiter = ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER;

	private boolean continueOnError = false;

	private boolean ignoreFailedDrops = false;


	/**
	 * Construct a new {@code ResourceDatabasePopulator} with default settings.
	 * @since 4.0.3
	 */
	public ResourceDatabasePopulator() {
		/* no-op */
	}

	/**
	 * Construct a new {@code ResourceDatabasePopulator} with default settings
	 * for the supplied scripts.
	 * @param scripts the scripts to execute to initialize or clean up the database;
	 * never {@code null}
	 * @since 4.0.3
	 */
	public ResourceDatabasePopulator(Resource... scripts) {
		this();
		setScripts(scripts);
	}

	/**
	 * Construct a new {@code ResourceDatabasePopulator} with the supplied values.
	 * @param continueOnError flag to indicate that all failures in SQL should be
	 * logged but not cause a failure
	 * @param ignoreFailedDrops flag to indicate that a failed SQL {@code DROP}
	 * statement can be ignored
	 * @param sqlScriptEncoding the encoding for the supplied SQL scripts; may
	 * be {@code null} or <em>empty</em> to indicate platform encoding
	 * @param scripts the scripts to execute to initialize or clean up the database;
	 * never {@code null}
	 * @since 4.0.3
	 */
	public ResourceDatabasePopulator(boolean continueOnError, boolean ignoreFailedDrops, String sqlScriptEncoding,
			Resource... scripts) {
		this(scripts);
		this.continueOnError = continueOnError;
		this.ignoreFailedDrops = ignoreFailedDrops;
		setSqlScriptEncoding(sqlScriptEncoding);
	}

	/**
	 * Add a script to execute to initialize or clean up the database.
	 * @param script the path to an SQL script; never {@code null}
	 */
	public void addScript(Resource script) {
		Assert.notNull(script, "Script must not be null");
		getScripts().add(script);
	}

	/**
	 * Add multiple scripts to execute to initialize or clean up the database.
	 * @param scripts the scripts to execute; never {@code null}
	 */
	public void addScripts(Resource... scripts) {
		assertContentsOfScriptArray(scripts);
		getScripts().addAll(Arrays.asList(scripts));
	}

	/**
	 * Set the scripts to execute to initialize or clean up the database,
	 * replacing any previously added scripts.
	 * @param scripts the scripts to execute; never {@code null}
	 */
	public void setScripts(Resource... scripts) {
		assertContentsOfScriptArray(scripts);
		// Ensure that the list is modifiable
		this.scripts = new ArrayList<Resource>(Arrays.asList(scripts));
	}

	/**
	 * Specify the encoding for the configured SQL scripts, if different from the
	 * platform encoding.
	 * @param sqlScriptEncoding the encoding used in scripts; may be {@code null}
	 * or empty to indicate platform encoding
	 * @see #addScript(Resource)
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = StringUtils.hasText(sqlScriptEncoding) ? sqlScriptEncoding : null;
	}

	/**
	 * Specify the statement separator, if a custom one.
	 * <p>Defaults to {@code ";"} if not specified and falls back to {@code "\n"}
	 * as a last resort; may be set to {@link ScriptUtils#EOF_STATEMENT_SEPARATOR}
	 * to signal that each script contains a single statement without a separator.
	 * @param separator the script statement separator
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Set the prefix that identifies single-line comments within the SQL scripts.
	 * <p>Defaults to {@code "--"}.
	 * @param commentPrefix the prefix for single-line comments
	 */
	public void setCommentPrefix(String commentPrefix) {
		this.commentPrefix = commentPrefix;
	}

	/**
	 * Set the start delimiter that identifies block comments within the SQL
	 * scripts.
	 * <p>Defaults to {@code "/*"}.
	 * @param blockCommentStartDelimiter the start delimiter for block comments;
	 * never {@code null} or empty
	 * @since 4.0.3
	 * @see #setBlockCommentEndDelimiter
	 */
	public void setBlockCommentStartDelimiter(String blockCommentStartDelimiter) {
		Assert.hasText(blockCommentStartDelimiter, "BlockCommentStartDelimiter must not be null or empty");
		this.blockCommentStartDelimiter = blockCommentStartDelimiter;
	}

	/**
	 * Set the end delimiter that identifies block comments within the SQL
	 * scripts.
	 * <p>Defaults to <code>"*&#47;"</code>.
	 * @param blockCommentEndDelimiter the end delimiter for block comments;
	 * never {@code null} or empty
	 * @since 4.0.3
	 * @see #setBlockCommentStartDelimiter
	 */
	public void setBlockCommentEndDelimiter(String blockCommentEndDelimiter) {
		Assert.hasText(blockCommentEndDelimiter, "BlockCommentEndDelimiter must not be null or empty");
		this.blockCommentEndDelimiter = blockCommentEndDelimiter;
	}

	/**
	 * Flag to indicate that all failures in SQL should be logged but not cause a failure.
	 * <p>Defaults to {@code false}.
	 * @param continueOnError {@code true} if script execution should continue on error
	 */
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	/**
	 * Flag to indicate that a failed SQL {@code DROP} statement can be ignored.
	 * <p>This is useful for a non-embedded database whose SQL dialect does not
	 * support an {@code IF EXISTS} clause in a {@code DROP} statement.
	 * <p>The default is {@code false} so that if the populator runs accidentally, it will
	 * fail fast if a script starts with a {@code DROP} statement.
	 * @param ignoreFailedDrops {@code true} if failed drop statements should be ignored
	 */
	public void setIgnoreFailedDrops(boolean ignoreFailedDrops) {
		this.ignoreFailedDrops = ignoreFailedDrops;
	}

	/**
	 * {@inheritDoc}
	 * @see #execute(DataSource)
	 */
	@Override
	public void populate(Connection connection) throws ScriptException {
		Assert.notNull(connection, "Connection must not be null");
		for (Resource script : getScripts()) {
			ScriptUtils.executeSqlScript(connection, encodeScript(script), this.continueOnError,
				this.ignoreFailedDrops, this.commentPrefix, this.separator, this.blockCommentStartDelimiter,
				this.blockCommentEndDelimiter);
		}
	}

	/**
	 * Execute this {@code ResourceDatabasePopulator} against the given
	 * {@link DataSource}.
	 * <p>Delegates to {@link DatabasePopulatorUtils#execute}.
	 * @param dataSource the {@code DataSource} to execute against; never {@code null}
	 * @throws ScriptException if an error occurs
	 * @since 4.1
	 * @see #populate(Connection)
	 */
	public void execute(DataSource dataSource) throws ScriptException {
		Assert.notNull(dataSource, "DataSource must not be null");
		DatabasePopulatorUtils.execute(this, dataSource);
	}

	final List<Resource> getScripts() {
		return this.scripts;
	}

	/**
	 * {@link EncodedResource} is not a sub-type of {@link Resource}. Thus we
	 * always need to wrap each script resource in an {@code EncodedResource}
	 * using the configured {@linkplain #setSqlScriptEncoding encoding}.
	 * @param script the script to wrap; never {@code null}
	 */
	private EncodedResource encodeScript(Resource script) {
		Assert.notNull(script, "Script must not be null");
		return new EncodedResource(script, this.sqlScriptEncoding);
	}

	private void assertContentsOfScriptArray(Resource... scripts) {
		Assert.notNull(scripts, "Scripts must not be null");
		Assert.noNullElements(scripts, "Scripts array must not contain null elements");
	}

}
