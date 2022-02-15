/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.observability;

import io.micrometer.api.instrument.observation.Observation;

/**
 * R2DBC {@link Observation.Context}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public final class R2dbcContext extends Observation.Context {
	private final String connectionName;

	private final String connectionFactoryName;

	private final String threadName;

	public R2dbcContext(String connectionName, String connectionFactoryName, String threadName) {
		this.connectionName = connectionName;
		this.connectionFactoryName = connectionFactoryName;
		this.threadName = threadName;
	}

	public String getConnectionName() {
		return this.connectionName;
	}

	public String getConnectionFactoryName() {
		return this.connectionFactoryName;
	}

	public String getThreadName() {
		return this.threadName;
	}
}
