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

import io.r2dbc.spi.Batch;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.Result;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

/**
 * Lazy implementation of {@link Batch}.
 *
 * <p>This implementation buffers SQL commands locally and defers creation
 * of the underlying R2DBC {@link Batch} until {@link #execute()} is called. Upon execution,
 * it triggers the initialization of the physical connection, creates a real batch,
 * replays the buffered commands, and executes them.
 *
 * @author Somil Jain
 * @since 6.2
 * @see LazyConnection
 */
class LazyBatch implements Batch {

	private final Mono<Connection> connectionMono;
	private final List<String> sqlCommands = new ArrayList<>();

	LazyBatch(Mono<Connection> connectionMono) {
		this.connectionMono = connectionMono;
	}

	/**
	 * Add a statement to this batch.
	 * <p>The SQL command is buffered locally and not sent to the database immediately.
	 * @param sql the statement to add
	 * @return this batch
	 */
	@Override
	public Batch add(String sql) {
		this.sqlCommands.add(sql);
		return this;
	}

	/**
	 * Execute the batch, triggering physical connection creation if necessary.
	 * <p>
	 * All buffered SQL commands are replayed onto a newly created
	 * {@link Batch} obtained from the physical {@link Connection}.
	 */
	@Override
	public Publisher<? extends Result> execute() {
		return this.connectionMono.flatMapMany(conn -> {
			Batch batch = conn.createBatch();
			for (String sql : this.sqlCommands) {
				batch.add(sql);
			}
			return batch.execute();
		});
	}
}