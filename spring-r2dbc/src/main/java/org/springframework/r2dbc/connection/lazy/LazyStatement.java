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

package org.springframework.r2dbc.connection.lazy;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lazy implementation of {@link Statement}.
 *
 * <p>This implementation buffers all statement configuration and parameter
 * bindings locally and defers creation of the underlying R2DBC {@link Statement}
 * until {@link #execute()} is invoked.
 *
 * <p>Upon execution, the physical {@link Connection} is obtained, a real
 * {@link Statement} is created, all buffered operations are replayed in order,
 * and the statement is executed.
 *
 * @author Somil Jain
 * @since 6.2
 * @see LazyConnection
 */
class LazyStatement implements Statement {

	private final String sql;
	private final Mono<Connection> connectionMono;
	private final List<Consumer<Statement>> operations = new ArrayList<>();

	LazyStatement(String sql, Mono<Connection> connectionMono) {
		this.sql = sql;
		this.connectionMono = connectionMono;
	}

	@Override
	public Statement add() {
		this.operations.add(Statement::add);
		return this;
	}

	@Override
	public Statement bind(int index, Object value) {
		this.operations.add(s -> s.bind(index, value));
		return this;
	}

	@Override
	public Statement bind(String name, Object value) {
		this.operations.add(s -> s.bind(name, value));
		return this;
	}

	@Override
	public Statement bindNull(int index, Class<?> type) {
		this.operations.add(s -> s.bindNull(index, type));
		return this;
	}

	@Override
	public Statement bindNull(String name, Class<?> type) {
		this.operations.add(s -> s.bindNull(name, type));
		return this;
	}

	@Override
	public Statement returnGeneratedValues(String... columns) {
		this.operations.add(s -> s.returnGeneratedValues(columns));
		return this;
	}

	@Override
	public Statement fetchSize(int rows) {
		this.operations.add(s -> s.fetchSize(rows));
		return this;
	}

	/**
	 * Execute the statement, triggering physical connection creation if necessary.
	 * <p>The actual statement is created using the original SQL, all buffered
	 * bindings and configurations are applied, and then executed.
	 */
	@Override
	public Publisher<? extends Result> execute() {
		return this.connectionMono.flatMapMany(conn -> {
			Statement actual = conn.createStatement(this.sql);

			for (Consumer<Statement> op : this.operations) {
				op.accept(actual);
			}

			return actual.execute();
		});
	}
}
