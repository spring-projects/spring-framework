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

package org.springframework.test.jdbc;

import org.springframework.util.StringUtils;

import java.util.List;
import java.io.LineNumberReader;
import java.io.IOException;

/**
 * JdbcTestUtils is a collection of JDBC related utility methods for
 * use in unit and integration testing scenarios.
 *
 * @author Thomas Risberg
 * @since 2.5.4
 */
public class JdbcTestUtils {

	/**
	 * Read a script from the LineNumberReaded and build a String containing the lines.
	 * @param lineNumberReader the <code>LineNumberReader</> containing the script to be processed
	 * @return <code>String</code> containing the script lines
	 * @throws IOException
	 */
	public static String readScript(LineNumberReader lineNumberReader) throws IOException {
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
	public static boolean containsSqlScriptDelimiters(String script, char delim) {
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
	public static void splitSqlScript(String script, char delim, List<String> statements) {
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
