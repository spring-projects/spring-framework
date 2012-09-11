/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.util.StringUtils;

/**
 * Populates a database from SQL scripts defined in external resources.
 *
 * <p>Call {@link #addScript(Resource)} to add a SQL script location.
 * Call {@link #setSqlScriptEncoding(String)} to set the encoding for all added scripts.
 *
 * @author Keith Donald
 * @author Dave Syer
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Oliver Gierke
 * @since 3.0
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

	private static String DEFAULT_COMMENT_PREFIX = "--";

	private static String DEFAULT_STATEMENT_SEPARATOR = ";";

	private static final Log logger = LogFactory.getLog(ResourceDatabasePopulator.class);


	private List<Resource> scripts = new ArrayList<Resource>();

	private String sqlScriptEncoding;

	private String separator;

	private String commentPrefix = DEFAULT_COMMENT_PREFIX;

	private boolean continueOnError = false;

	private boolean ignoreFailedDrops = false;


	/**
	 * Add a script to execute to populate the database.
	 * @param script the path to a SQL script
	 */
	public void addScript(Resource script) {
		this.scripts.add(script);
	}

	/**
	 * Set the scripts to execute to populate the database.
	 * @param scripts the scripts to execute
	 */
	public void setScripts(Resource[] scripts) {
		this.scripts = Arrays.asList(scripts);
	}

	/**
	 * Specify the encoding for SQL scripts, if different from the platform encoding.
	 * Note setting this property has no effect on added scripts that are already
	 * {@link EncodedResource encoded resources}.
	 * @see #addScript(Resource)
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	/**
	 * Specify the statement separator, if a custom one. Default is ";".
	 */
	public void setSeparator(String separator) {
		this.separator = separator;
	}

	/**
	 * Set the line prefix that identifies comments in the SQL script.
	 * Default is "--".
	 */
	public void setCommentPrefix(String commentPrefix) {
		this.commentPrefix = commentPrefix;
	}

	/**
	 * Flag to indicate that all failures in SQL should be logged but not cause a failure.
	 * Defaults to false.
	 */
	public void setContinueOnError(boolean continueOnError) {
		this.continueOnError = continueOnError;
	}

	/**
	 * Flag to indicate that a failed SQL <code>DROP</code> statement can be ignored.  
	 * <p>This is useful for non-embedded databases whose SQL dialect does not support an
	 * <code>IF EXISTS</code> clause in a <code>DROP</code>. The default is false so that if the
	 * populator runs accidentally, it will fail fast when the script starts with a <code>DROP</code>.
	 */
	public void setIgnoreFailedDrops(boolean ignoreFailedDrops) {
		this.ignoreFailedDrops = ignoreFailedDrops;
	}


	public void populate(Connection connection) throws SQLException {
		for (Resource script : this.scripts) {
			executeSqlScript(connection, applyEncodingIfNecessary(script), this.continueOnError, this.ignoreFailedDrops);
		}
	}

	private EncodedResource applyEncodingIfNecessary(Resource script) {
		if (script instanceof EncodedResource) {
			return (EncodedResource) script;
		}
		else {
			return new EncodedResource(script, this.sqlScriptEncoding);
		}
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any {@link #setSeparator(String) statement separators} will be removed.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param connection the JDBC Connection with which to perform JDBC operations
	 * @param resource the resource (potentially associated with a specific encoding) to load the SQL script from
	 * @param continueOnError whether or not to continue without throwing an exception in the event of an error
	 * @param ignoreFailedDrops whether of not to continue in the event of specifically an error on a <code>DROP</code>
	 */
	private void executeSqlScript(Connection connection, EncodedResource resource,
			boolean continueOnError, boolean ignoreFailedDrops) throws SQLException {

		if (logger.isInfoEnabled()) {
			logger.info("Executing SQL script from " + resource);
		}
		long startTime = System.currentTimeMillis();
		List<String> statements = new LinkedList<String>();
		String script;
		try {
			script = readScript(resource);
		}
		catch (IOException ex) {
			throw new CannotReadScriptException(resource, ex);
		}
		String delimiter = this.separator;
		if (delimiter == null) {
			delimiter = DEFAULT_STATEMENT_SEPARATOR;
			if (!containsSqlScriptDelimiters(script, delimiter)) {
				delimiter = "\n";
			}
		}
		splitSqlScript(script, delimiter, statements);
		int lineNumber = 0;
		Statement stmt = connection.createStatement();
		try {
			for (String statement : statements) {
				lineNumber++;
				try {
					stmt.execute(statement);
					int rowsAffected = stmt.getUpdateCount();
					if (logger.isDebugEnabled()) {
						logger.debug(rowsAffected + " returned as updateCount for SQL: " + statement);
					}
				}
				catch (SQLException ex) {
					boolean dropStatement = StringUtils.startsWithIgnoreCase(statement.trim(), "drop");
					if (continueOnError || (dropStatement && ignoreFailedDrops)) {
						if (logger.isDebugEnabled()) {
							logger.debug("Failed to execute SQL script statement at line " + lineNumber +
									" of resource " + resource + ": " + statement, ex);
						}
					}
					else {
						throw new ScriptStatementFailedException(statement, lineNumber, resource, ex);
					}
				}
			}
		}
		finally {
			try {
				stmt.close();
			}
			catch (Throwable ex) {
				logger.debug("Could not close JDBC Statement", ex);
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		if (logger.isInfoEnabled()) {
			logger.info("Done executing SQL script from " + resource + " in " + elapsedTime + " ms.");
		}
	}

	/**
	 * Read a script from the given resource and build a String containing the lines.
	 * @param resource the resource to be read
	 * @return <code>String</code> containing the script lines
	 * @throws IOException in case of I/O errors
	 */
	private String readScript(EncodedResource resource) throws IOException {
		LineNumberReader lnr = new LineNumberReader(resource.getReader());
		String currentStatement = lnr.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (StringUtils.hasText(currentStatement) &&
					(this.commentPrefix != null && !currentStatement.startsWith(this.commentPrefix))) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lnr.readLine();
		}
		maybeAddSeparatorToScript(scriptBuilder);
		return scriptBuilder.toString();
	}

	private void maybeAddSeparatorToScript(StringBuilder scriptBuilder) {
		if (this.separator == null) {
			return;
		}
		String trimmed = this.separator.trim();
		if (trimmed.length() == this.separator.length()) {
			return;
		}
		// separator ends in whitespace, so we might want to see if the script is trying to end the same way
		if (scriptBuilder.lastIndexOf(trimmed) == scriptBuilder.length() - trimmed.length()) {
			scriptBuilder.append(this.separator.substring(trimmed.length()));
		}
	}

	/**
	 * Does the provided SQL script contain the specified delimiter?
	 * @param script the SQL script
	 * @param delim character delimiting each statement - typically a ';' character
	 */
	private boolean containsSqlScriptDelimiters(String script, String delim) {
		boolean inLiteral = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			if (content[i] == '\'') {
				inLiteral = !inLiteral;
			}
			if (!inLiteral && startsWithDelimiter(script, i, delim)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return whether the substring of a given source {@link String} starting at the
	 * given index starts with the given delimiter.
	 *
	 * @param source the source {@link String} to inspect
	 * @param startIndex the index to look for the delimiter
	 * @param delim the delimiter to look for
	 */
	private boolean startsWithDelimiter(String source, int startIndex, String delim) {

		int endIndex = startIndex + delim.length();

		if (source.length() < endIndex) {
			// String is too short to contain the delimiter
			return false;
		}

		return source.substring(startIndex, endIndex).equals(delim);
	}

	/**
	 * Split an SQL script into separate statements delimited with the provided delimiter
	 * character. Each individual statement will be added to the provided {@code List}.
	 *
	 * @param script the SQL script
	 * @param delim character delimiting each statement (typically a ';' character)
	 * @param statements the List that will contain the individual statements
	 */
	private void splitSqlScript(String script, String delim, List<String> statements) {
		StringBuilder sb = new StringBuilder();
		boolean inLiteral = false;
		boolean inEscape = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			char c = content[i];
			if (inEscape) {
				inEscape = false;
				sb.append(c);
				continue;
			}
			// MySQL style escapes
			if (c == '\\') {
				inEscape = true;
				sb.append(c);
				continue;
			}
			if (c == '\'') {
				inLiteral = !inLiteral;
			}
			if (!inLiteral) {
				if (startsWithDelimiter(script, i, delim)) {
					if (sb.length() > 0) {
						statements.add(sb.toString());
						sb = new StringBuilder();
					}
					i += delim.length() - 1;
					continue;
				}
				else if (c == '\n' || c == '\t') {
					c = ' ';
				}
			}
			sb.append(c);
		}
		if (StringUtils.hasText(sb)) {
			statements.add(sb.toString());
		}
	}

}
