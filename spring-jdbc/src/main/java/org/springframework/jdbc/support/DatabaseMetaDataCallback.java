/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

/**
 * A callback interface used by the JdbcUtils class. Implementations of this
 * interface perform the actual work of extracting database meta-data, but
 * don't need to worry about exception handling. SQLExceptions will be caught
 * and handled correctly by the JdbcUtils class.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @param <T> the result type
 * @see JdbcUtils#extractDatabaseMetaData(javax.sql.DataSource, DatabaseMetaDataCallback)
 */
@FunctionalInterface
public interface DatabaseMetaDataCallback<T extends @Nullable Object> {

	/**
	 * Implementations must implement this method to process the meta-data
	 * passed in. Exactly what the implementation chooses to do is up to it.
	 * @param dbmd the DatabaseMetaData to process
	 * @return a result object extracted from the meta-data
	 * (can be an arbitrary object, as needed by the implementation)
	 * @throws SQLException if an SQLException is encountered getting
	 * column values (that is, there's no need to catch SQLException)
	 * @throws MetaDataAccessException in case of other failures while
	 * extracting meta-data (for example, reflection failure)
	 */
	T processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException;

}
