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

package org.springframework.jdbc.support;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/** 
 * A callback interface used by the JdbcUtils class. Implementations of this
 * interface perform the actual work of extracting database meta data, but
 * don't need to worry about exception handling. SQLExceptions will be caught
 * and handled correctly by the JdbcUtils class.
 *
 * @author Thomas Risberg
 * @see JdbcUtils#extractDatabaseMetaData
 */
public interface DatabaseMetaDataCallback {
	
	/** 
	 * Implementations must implement this method to process the meta data
	 * passed in. Exactly what the implementation chooses to do is up to it.
	 * @param dbmd the DatabaseMetaData to process
	 * @return a result object extracted from the meta data
	 * (can be an arbitrary object, as needed by the implementation)
	 * @throws SQLException if a SQLException is encountered getting
	 * column values (that is, there's no need to catch SQLException)
	 * @throws MetaDataAccessException in case of other failures while
	 * extracting meta data (for example, reflection failure)
	 */
	Object processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException;

}
