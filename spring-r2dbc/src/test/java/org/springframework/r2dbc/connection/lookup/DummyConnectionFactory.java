/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.lookup;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import org.reactivestreams.Publisher;

/**
 * Stub, do-nothing {@link ConnectionFactory} implementation.
 *
 * <p>All methods throw {@link UnsupportedOperationException}.
 *
 * @author Mark Paluch
 */
class DummyConnectionFactory implements ConnectionFactory {

	@Override
	public Publisher<? extends Connection> create() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ConnectionFactoryMetadata getMetadata() {
		throw new UnsupportedOperationException();
	}

}
