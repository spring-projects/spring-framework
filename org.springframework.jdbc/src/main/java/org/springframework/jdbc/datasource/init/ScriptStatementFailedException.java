/*
 * Copyright 2002-2010 the original author or authors.
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
 * Thrown by {@link ResourceDatabasePopulator} if a statement in one of its SQL scripts
 * failed when executing it against the target database.
 *
 * @author Juergen Hoeller
 * @since 3.0.5
 */
public class ScriptStatementFailedException extends RuntimeException {

	/**
	 * Constructor a new ScriptStatementFailedException.
	 * @param statement the actual SQL statement that failed
	 * @param lineNumber the line number in the SQL script
	 * @param resource the resource that could not be read from
	 * @param cause the underlying cause of the resource access failure
	 */
	public ScriptStatementFailedException(String statement, int lineNumber, EncodedResource resource, Throwable cause) {
		super("Failed to execute SQL script statement at line " + lineNumber +
				" of resource " + resource + ": " + statement, cause);
	}

}
