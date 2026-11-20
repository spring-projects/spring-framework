/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource.lookup;

import java.sql.Connection;

import org.springframework.jdbc.datasource.AbstractDataSource;

/**
 * Stub, do-nothing DataSource implementation.
 *
 * <p>All methods throw {@link UnsupportedOperationException}.
 *
 * @author Rick Evans
 */
class StubDataSource extends AbstractDataSource {

	@Override
	public Connection getConnection() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Connection getConnection(String username, String password) {
		throw new UnsupportedOperationException();
	}

}
