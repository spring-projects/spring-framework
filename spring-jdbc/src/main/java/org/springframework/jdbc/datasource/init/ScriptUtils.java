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

package org.springframework.jdbc.datasource.init;

import java.io.IOException;
import java.io.LineNumberReader;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.util.StringUtils;


/**
 * Generic utility methods for working with SQL scripts. Mainly for 
 * internal use within the framework.
 * 
 * @author Chris Baldwin
 * @since 4.0.3
 */
public abstract class ScriptUtils {

	public static final String DEFAULT_COMMENT_PREFIX = "--";

	public static final char DEFAULT_STATEMENT_SEPARATOR_CHAR = ';';
	
	public static final String DEFAULT_STATEMENT_SEPARATOR = String.valueOf(DEFAULT_STATEMENT_SEPARATOR_CHAR);
	
	public static final String DEFAULT_BLOCK_COMMENT_START_DELIMITER = "/*";
	
	public static final String DEFAULT_BLOCK_COMMENT_END_DELIMITER = "*/";

	private static final Log logger = LogFactory.getLog(ScriptUtils.class);
	
	/**
	 * Split an SQL script into separate statements delimited by the provided
	 * delimiter character. Each individual statement will be added to the
	 * provided {@code List}.
	 * <p>Within a statement, "{@code --}" will be used as the comment prefix;
	 * any text beginning with the comment prefix and extending to the end of
	 * the line will be omitted from the statement. In addition, multiple adjacent
	 * whitespace characters will be collapsed into a single space.
	 * @param script the SQL script
	 * @param delim character delimiting each statement &mdash; typically a ';' character
	 * @param statements the list that will contain the individual statements
	 * @since 4.0.3
	 */
	public static void splitSqlScript(String script, char delim, List<String> statements) {
		splitSqlScript(script, String.valueOf(delim), DEFAULT_COMMENT_PREFIX, DEFAULT_BLOCK_COMMENT_START_DELIMITER,
				DEFAULT_BLOCK_COMMENT_END_DELIMITER, statements);
	}

	/**
	 * Split an SQL script into separate statements delimited by the provided
	 * delimiter string. Each individual statement will be added to the provided
	 * {@code List}.
	 * <p>Within a statement, the provided {@code commentPrefix} will be honored;
	 * any text beginning with the comment prefix and extending to the end of the
	 * line will be omitted from the statement. In addition, multiple adjacent
	 * whitespace characters will be collapsed into a single space.
	 * @param script the SQL script
	 * @param delim character delimiting each statement &mdash; typically a ';' character
	 * @param commentPrefix the prefix that identifies line comments in the SQL script &mdash; typically "--"
	 * @param statements the List that will contain the individual statements
	 * @since 4.0.3
	 */
	public static void splitSqlScript(String script, String delim, String commentPrefix, String blockCommentOpen,
			String blockCommentClose, List<String> statements) {
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
				if (script.startsWith(delim, i)) {
					// we've reached the end of the current statement
					if (sb.length() > 0) {
						statements.add(sb.toString());
						sb = new StringBuilder();
					}
					i += delim.length() - 1;
					continue;
				}
				else if (script.startsWith(commentPrefix, i)) {
					// skip over any content from the start of the comment to the EOL
					int indexOfNextNewline = script.indexOf("\n", i);
					if (indexOfNextNewline > i) {
						i = indexOfNextNewline;
						continue;
					}
					else {
						// if there's no EOL, we must be at the end
						// of the script, so stop here.
						break;
					}
				}
				else if (script.startsWith(blockCommentOpen, i)) {
					// skip over any block comments
					int indexOfCommentClose = script.indexOf(blockCommentClose, i);
					if (indexOfCommentClose > i) {
						i = indexOfCommentClose + blockCommentClose.length() - 1;
						continue;
					}
					else {
						throw new BadSqlGrammarException("", script.substring(i), 
								new SQLException("Missing block comment end delimiter")); 
					}
				}
				else if (c == ' ' || c == '\n' || c == '\t') {
					// avoid multiple adjacent whitespace characters
					if (sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ') {
						c = ' ';
					}
					else {
						continue;
					}
				}
			}
			sb.append(c);
		}
		if (StringUtils.hasText(sb)) {
			statements.add(sb.toString());
		}
	}

	/**
	 * Read a script from the provided {@code LineNumberReader}, using
	 * "{@code --}" as the comment prefix, and build a {@code String} containing
	 * the lines.
	 * @param lineNumberReader the {@code LineNumberReader} containing the script
	 * to be processed
	 * @return a {@code String} containing the script lines
	 * @see #readScript(LineNumberReader, String, String)
	 * @since 4.0.3
	 */
	public static String readScript(LineNumberReader lineNumberReader) throws IOException {
		return readScript(lineNumberReader, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR);
	}

	/**
	 * Read a script from the given resource, using "{@code --}" as the comment prefix 
	 * and "{@code ;} as the statement separator, and build a String containing the lines.
	 * @param resource the {@code EncodedResource} to be read
	 * @return {@code String} containing the script lines
	 * @throws IOException in case of I/O errors
	 * @since 4.0.3
	 */
	public static String readScript(EncodedResource resource) throws IOException {
		LineNumberReader lnr = new LineNumberReader(resource.getReader());
		try {
			return readScript(lnr, DEFAULT_COMMENT_PREFIX, DEFAULT_STATEMENT_SEPARATOR);
		}
		finally {
			lnr.close();
		}
	}
	
	/**
	 * Read a script from the provided resource, using the supplied
	 * comment prefix and statement separator, and build a {@code String} containing the lines.
	 * <p>Lines <em>beginning</em> with the comment prefix are excluded from the
	 * results; however, line comments anywhere else &mdash; for example, within
	 * a statement &mdash; will be included in the results.
	 * @param resource the {@code EncodedResource} containing the script
	 * to be processed
	 * @param commentPrefix the prefix that identifies comments in the SQL script &mdash; typically "--"
	 * @param separator the statement separator in the SQL script &mdash; typically ";"
	 * @return a {@code String} containing the script lines
	 * @since 4.0.3
	 */
	public static String readScript(EncodedResource resource, String commentPrefix, 
			String separator) throws IOException {
		LineNumberReader lnr = new LineNumberReader(resource.getReader());
		try {
			return readScript(lnr, commentPrefix, separator);
		}
		finally {
			lnr.close();
		}
	}
	
	/**
	 * Read a script from the provided {@code LineNumberReader}, using the supplied
	 * comment prefix and statement separator, and build a {@code String} containing the lines.
	 * <p>Lines <em>beginning</em> with the comment prefix are excluded from the
	 * results; however, line comments anywhere else &mdash; for example, within
	 * a statement &mdash; will be included in the results.
	 * @param lineNumberReader the {@code LineNumberReader} containing the script
	 * to be processed
	 * @param commentPrefix the prefix that identifies comments in the SQL script &mdash; typically "--"
	 * @param separator the statement separator in the SQL script &mdash; typically ";"
	 * @return a {@code String} containing the script lines
	 * @since 4.0.3
	 */
	public static String readScript(LineNumberReader lineNumberReader, String commentPrefix, 
			String separator) throws IOException {
		String currentStatement = lineNumberReader.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (StringUtils.hasText(currentStatement) &&
					(commentPrefix != null && !currentStatement.startsWith(commentPrefix))) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lineNumberReader.readLine();
		}
		maybeAddSeparatorToScript(scriptBuilder, separator);
		return scriptBuilder.toString();
	}

	private static void maybeAddSeparatorToScript(StringBuilder scriptBuilder, String separator) {
		if (separator == null) {
			return;
		}
		String trimmed = separator.trim();
		if (trimmed.length() == separator.length()) {
			return;
		}
		// separator ends in whitespace, so we might want to see if the script is trying
		// to end the same way
		if (scriptBuilder.lastIndexOf(trimmed) == scriptBuilder.length() - trimmed.length()) {
			scriptBuilder.append(separator.substring(trimmed.length()));
		}
	}

	/**
	 * Does the provided SQL script contain the specified delimiter?
	 * @param script the SQL script
	 * @param delim character delimiting each statement - typically a ';' character
	 * @since 4.0.3
	 */
	public static boolean containsSqlScriptDelimiters(String script, char delim) {
		return containsSqlScriptDelimiters(script, String.valueOf(delim));
	}
	
	/**
	 * Does the provided SQL script contain the specified delimiter?
	 * @param script the SQL script
	 * @param delim String delimiting each statement - typically a ';' character
	 * @since 4.0.3
	 */
	public static boolean containsSqlScriptDelimiters(String script, String delim) {
		boolean inLiteral = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			if (content[i] == '\'') {
				inLiteral = !inLiteral;
			}
			if (!inLiteral && script.startsWith(delim, i)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Execute the given SQL script.
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any statement separators will be removed.
	 * <p><b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param executor the {@code ScriptStatementExecutor} with which to perform JDBC operations
	 * @param resource the resource (potentially associated with a specific encoding) to load the SQL script from
	 * @param continueOnError whether or not to continue without throwing an exception in the event of an error
	 * @param ignoreFailedDrops whether of not to continue in the event of specifically an error on a {@code DROP}
	 * @param commentPrefix the script line comment prefix
	 * if not specified 
	 * @param separator the script statement separator, defaults to {@code DEFAUT_STATEMENT_SEPARATOR}
	 * if not specified 
	 * @param blockCommentStartDelim the script block comment starting delimiter
	 * @param blockCommentEndDelim the script block comment ending delimiter
	 * @since 4.0.3
	 */
	public static void executeSqlScript(ScriptStatementExecutor executor, EncodedResource resource, 
			boolean continueOnError, boolean ignoreFailedDrops, String commentPrefix, String separator,
			String blockCommentStartDelim, String blockCommentEndDelim) throws DataAccessException {

		if (logger.isInfoEnabled()) {
			logger.info("Executing SQL script from " + resource);
		}
		long startTime = System.currentTimeMillis();
		List<String> statements = new LinkedList<String>();
		String script;
		try {
			script = readScript(resource, commentPrefix, separator);
		}
		catch (IOException ex) {
			throw new CannotReadScriptException(resource, ex);
		}
		if (separator == null) {
			separator = DEFAULT_STATEMENT_SEPARATOR;
			if (!containsSqlScriptDelimiters(script, separator)) {
				separator = "\n";
			}
		}
		splitSqlScript(script, separator, commentPrefix, blockCommentStartDelim, blockCommentEndDelim, statements);
		int lineNumber = 0;
		for (String statement : statements) {
			lineNumber++;
			try {
				int rowsAffected = executor.executeScriptStatement(statement);
				if (logger.isDebugEnabled()) {
					logger.debug(rowsAffected + " returned as updateCount for SQL: " + statement);
				}
			}
			catch (DataAccessException ex) {
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
		long elapsedTime = System.currentTimeMillis() - startTime;
		if (logger.isInfoEnabled()) {
			logger.info("Done executing SQL script from " + resource + " in " + elapsedTime + " ms.");
		}
	}

	/**
	 * Interface to be implemented by an object so that {@code executeScript()} is able to use 
	 * it to execute script statements.
	 * @since 4.0.3
	 */
	public interface ScriptStatementExecutor
	{
		/**
		 * Execute the given SQL statement and return a count of the number of affected rows.
		 * @return the number of rows affected by the statement
		 * @throws DataAccessException if there is a problem during statement execution
		 */
		public int executeScriptStatement(String statement) throws DataAccessException;
	}

}
