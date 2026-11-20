/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import org.jspecify.annotations.Nullable;

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
	 * @return the SQL string, or {@code null} if not available
	 */
	@Nullable String getSql();

}
