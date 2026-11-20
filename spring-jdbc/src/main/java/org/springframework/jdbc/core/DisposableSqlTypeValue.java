/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

/**
 * Subinterface of {@link SqlTypeValue} that adds a cleanup callback,
 * to be invoked after the value has been set and the corresponding
 * statement has been executed.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see org.springframework.jdbc.core.support.SqlLobValue
 */
public interface DisposableSqlTypeValue extends SqlTypeValue {

	/**
	 * Clean up resources held by this type value,
	 * for example the LobCreator in case of an SqlLobValue.
	 * @see org.springframework.jdbc.core.support.SqlLobValue#cleanup()
	 * @see org.springframework.jdbc.support.SqlValue#cleanup()
	 */
	void cleanup();

}
