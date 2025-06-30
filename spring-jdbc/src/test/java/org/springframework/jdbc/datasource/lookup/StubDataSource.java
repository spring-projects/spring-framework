/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
