/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.jdbc.datasource.embedded;

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
 * <p>
 * Call {@link #addScript(Resource)} to add a SQL script location.<br>
 * Call {@link #setSqlScriptEncoding(String)} to set the encoding for all added scripts.<br>
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

	private static final Log logger = LogFactory.getLog(ResourceDatabasePopulator.class);

	private List<Resource> scripts = new ArrayList<Resource>();

	private String sqlScriptEncoding;

	/**
	 * Add a script to execute to populate the database.
	 * @param script the path to a SQL script
	 */
	public void addScript(Resource script) {
		scripts.add(script);
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
	 * Note setting this property has no effect on added scripts that are already {@link EncodedResource encoded resources}.
	 * @see #addScript(Resource)
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}
	
	public void populate(Connection connection) throws SQLException {
		for (Resource script : scripts) {
			executeSqlScript(connection, applyEncodingIfNecessary(script), false);
		}
	}

	private EncodedResource applyEncodingIfNecessary(Resource script) {
		if (script instanceof EncodedResource) {
			return (EncodedResource) script;
		} else {
			return new EncodedResource(script, sqlScriptEncoding);
		}
	}
	
	/**
	 * Execute the given SQL script. <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any semicolons will be removed. <b>Do not use this method to execute DDL if you expect rollback.</b>
	 * @param template the SimpleJdbcTemplate with which to perform JDBC operations
	 * @param resource the resource (potentially associated with a specific encoding) to load the SQL script from.
	 * @param continueOnError whether or not to continue without throwing an exception in the event of an error.
	 */
	private void executeSqlScript(Connection connection, EncodedResource resource, boolean continueOnError)
			throws SQLException {
		if (logger.isInfoEnabled()) {
			logger.info("Executing SQL script from " + resource);
		}
		long startTime = System.currentTimeMillis();
		List<String> statements = new LinkedList<String>();
		String script;
		try {
			script = readScript(resource);
		} catch (IOException e) {
			throw new CannotReadScriptException(resource, e);
		}
		char delimiter = ';';
		if (!containsSqlScriptDelimiters(script, delimiter)) {
			delimiter = '\n';
		}
		splitSqlScript(script, delimiter, statements);
		int lineNumber = 0;
		for (String statement : statements) {
			lineNumber++;
			Statement stmt = null;
			try {
				stmt = connection.createStatement();
				int rowsAffected = stmt.executeUpdate(statement);
				if (logger.isDebugEnabled()) {
					logger.debug(rowsAffected + " rows affected by SQL: " + statement);
				}
			} catch (SQLException e) {
				if (continueOnError) {
					if (logger.isWarnEnabled()) {
						logger.warn("Line " + lineNumber + " statement failed: " + statement, e);
					}
				} else {
					throw e;
				}
			} finally {
				JdbcUtils.closeStatement(stmt);
			}
		}
		long elapsedTime = System.currentTimeMillis() - startTime;
		if (logger.isInfoEnabled()) {
			logger.info("Done executing SQL script from " + resource + " in " + elapsedTime + " ms.");
		}
	}

	/**
	 * Read a script from the LineNumberReader and build a String containing the lines.
	 * @param lineNumberReader the <code>LineNumberReader</> containing the script to be processed
	 * @return <code>String</code> containing the script lines
	 * @throws IOException
	 */
	private static String readScript(EncodedResource resource) throws IOException {
		LineNumberReader lnr = new LineNumberReader(resource.getReader());
		String currentStatement = lnr.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (StringUtils.hasText(currentStatement)) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lnr.readLine();
		}
		return scriptBuilder.toString();
	}

	/**
	 * Does the provided SQL script contain the specified delimiter?
	 * @param script the SQL script
	 * @param delim charecter delimiting each statement - typically a ';' character
	 */
	private static boolean containsSqlScriptDelimiters(String script, char delim) {
		boolean inLiteral = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			if (content[i] == '\'') {
				inLiteral = !inLiteral;
			}
			if (content[i] == delim && !inLiteral) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Split an SQL script into separate statements delimited with the provided delimiter character. Each individual
	 * statement will be added to the provided <code>List</code>.
	 * @param script the SQL script
	 * @param delim charecter delimiting each statement - typically a ';' character
	 * @param statements the List that will contain the individual statements
	 */
	private static void splitSqlScript(String script, char delim, List<String> statements) {
		StringBuilder sb = new StringBuilder();
		boolean inLiteral = false;
		char[] content = script.toCharArray();
		for (int i = 0; i < script.length(); i++) {
			if (content[i] == '\'') {
				inLiteral = !inLiteral;
			}
			if (content[i] == delim && !inLiteral) {
				if (sb.length() > 0) {
					statements.add(sb.toString());
					sb = new StringBuilder();
				}
			} else {
				sb.append(content[i]);
			}
		}
		if (sb.length() > 0) {
			statements.add(sb.toString());
		}
	}

}