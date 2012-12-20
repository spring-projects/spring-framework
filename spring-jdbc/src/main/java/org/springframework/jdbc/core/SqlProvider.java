/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.jdbc.core;

/**
 * Interface to be implemented by objects that can provide SQL strings.
 *
 * <p>Typically implemented by PreparedStatementCreators, CallableStatementCreators
 * and StatementCallbacks that want to expose the SQL they use to create their
 * statements, to allow for better contextual information in case of exceptions.
 *
 * @author Juergen Hoeller
 * @since 16.03.2004
 * @see PreparedStatementCreator
 * @see CallableStatementCreator
 * @see StatementCallback
 */
public interface SqlProvider {

	/**
	 * Return the SQL string for this object, i.e.
	 * typically the SQL used for creating statements.
	 * @return the SQL string, or <code>null</code>
	 */
	String getSql();

}
