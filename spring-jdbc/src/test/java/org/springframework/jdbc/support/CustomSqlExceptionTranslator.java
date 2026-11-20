/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessResourceException;

/**
 * Custom SQLException translation for testing.
 *
 * @author Thomas Risberg
 */
public class CustomSqlExceptionTranslator implements SQLExceptionTranslator {

	@Override
	public DataAccessException translate(String task, @Nullable String sql, SQLException ex) {
		if (ex.getErrorCode() == 2) {
			return new TransientDataAccessResourceException("Custom", ex);
		}
		return null;
	}

}
