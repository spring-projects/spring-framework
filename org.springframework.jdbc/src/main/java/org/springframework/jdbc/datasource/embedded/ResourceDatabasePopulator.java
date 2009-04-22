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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

/**
 * Populates a database from schema and test-data SQL defined in external resources.
 * By default, looks for a schema.sql file and test-data.sql resource in the root of the classpath.
 * 
 * May be configured.
 * Call {@link #setSchemaLocation(Resource)} to configure the location of the database schema file.
 * Call {@link #setTestDataLocation(Resource)} to configure the location of the test data file.
 * Call {@link #setSqlScriptEncoding(String)} to set the encoding for the schema and test data SQL.
 */
public class ResourceDatabasePopulator implements DatabasePopulator {

	private static final Log logger = LogFactory.getLog(ResourceDatabasePopulator.class);

	private String sqlScriptEncoding;

	private List<Resource> scripts = new ArrayList<Resource>();
	
	/**
	 * Add a script to execute to populate the database.
	 * @param script the path to a SQL script
	 */
	public void addScript(Resource script) {
		scripts.add(script);
	}
	
	/**
	 * Specify the encoding for SQL scripts, if different from the platform encoding.
	 */
	public void setSqlScriptEncoding(String sqlScriptEncoding) {
		this.sqlScriptEncoding = sqlScriptEncoding;
	}

	public void populate(JdbcTemplate template) {
		for (Resource script : scripts) {
			executeSqlScript(template, new EncodedResource(script, sqlScriptEncoding), false);
		}
	}

	// From SimpleJdbcTestUtils - TODO address duplication
	
	/**
	 * Execute the given SQL script.
	 * <p>The script will normally be loaded by classpath. There should be one statement
	 * per line. Any semicolons will be removed. <b>Do not use this method to execute
	 * DDL if you expect rollback.</b>
	 * @param template the SimpleJdbcTemplate with which to perform JDBC operations
	 * @param resource the resource (potentially associated with a specific encoding)
	 * to load the SQL script from.
	 * @param continueOnError whether or not to continue without throwing an
	 * exception in the event of an error.
	 */
	static void executeSqlScript(JdbcTemplate template, EncodedResource resource, boolean continueOnError) {
		if (logger.isInfoEnabled()) {
			logger.info("Executing SQL script from " + resource);
		}
		long startTime = System.currentTimeMillis();
		List<String> statements = new LinkedList<String>();
		try {
			LineNumberReader lnr = new LineNumberReader(resource.getReader());
			String script = readScript(lnr);
			char delimiter = ';';
			if (!containsSqlScriptDelimiters(script, delimiter)) {
				delimiter = '\n';			
			}
			splitSqlScript(script, delimiter, statements);
			for (String statement : statements) {
				try {
					int rowsAffected = template.update(statement);
					if (logger.isDebugEnabled()) {
						logger.debug(rowsAffected + " rows affected by SQL: " + statement);
					}
				}
				catch (DataAccessException ex) {
					if (continueOnError) {
						if (logger.isWarnEnabled()) {
							logger.warn("SQL: " + statement + " failed", ex);
						}
					}
					else {
						throw ex;
					}
				}
			}
			long elapsedTime = System.currentTimeMillis() - startTime;
			if (logger.isInfoEnabled()) {
				logger.info("Done executing SQL script from " + resource + " in " + elapsedTime + " ms.");
			}
		}
		catch (IOException ex) {
			throw new DataAccessResourceFailureException("Failed to open SQL script from " + resource, ex);
		}
	}
	
	// From JdbcTestUtils - TODO address duplication - these do not seem as useful as the one above

	/**
	 * Read a script from the LineNumberReader and build a String containing the lines.
	 * @param lineNumberReader the <code>LineNumberReader</> containing the script to be processed
	 * @return <code>String</code> containing the script lines
	 * @throws IOException
	 */
	private static String readScript(LineNumberReader lineNumberReader) throws IOException {
		String currentStatement = lineNumberReader.readLine();
		StringBuilder scriptBuilder = new StringBuilder();
		while (currentStatement != null) {
			if (StringUtils.hasText(currentStatement)) {
				if (scriptBuilder.length() > 0) {
					scriptBuilder.append('\n');
				}
				scriptBuilder.append(currentStatement);
			}
			currentStatement = lineNumberReader.readLine();
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
	 * Split an SQL script into separate statements delimited with the provided delimiter character. Each
	 * individual statement will be added to the provided <code>List</code>.
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
			}
			else {
				sb.append(content[i]);
			}
		}
		if (sb.length() > 0) {
			statements.add(sb.toString());
		}
	}

}