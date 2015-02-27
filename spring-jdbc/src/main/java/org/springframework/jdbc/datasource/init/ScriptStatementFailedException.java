/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.core.io.support.EncodedResource;

/**
 * Thrown by {@link ScriptUtils} if a statement in an SQL script failed when
 * executing it against the target database.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0.5
 */
@SuppressWarnings("serial")
public class ScriptStatementFailedException extends ScriptException {

	/**
	 * Construct a new {@code ScriptStatementFailedException}.
	 * @param statement the actual SQL statement that failed
	 * @param statementNumber the statement number in the SQL script (i.e.,
	 * the nth statement present in the resource)
	 * @param resource the resource from which the SQL statement was read
	 * @param cause the underlying cause of the failure
	 */
	public ScriptStatementFailedException(String statement, int statementNumber, EncodedResource resource, Throwable cause) {
		super(buildErrorMessage(statement, statementNumber, resource), cause);
	}

	/**
	 * Build an error message based on the supplied parameters.
	 * @param statement the actual SQL statement that failed
	 * @param statementNumber the statement number in the SQL script (i.e.,
	 * the nth statement present in the resource)
	 * @param encodedResource the resource from which the SQL statement was read
	 */
	public static String buildErrorMessage(String statement, int statementNumber, EncodedResource encodedResource) {
		return String.format("Failed to execute SQL script statement #%s of %s: %s",
				statementNumber, encodedResource, statement);
	}

}
