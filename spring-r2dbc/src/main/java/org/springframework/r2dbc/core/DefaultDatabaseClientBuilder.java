/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.r2dbc.core;

import java.util.function.Consumer;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;

import org.springframework.lang.Nullable;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindMarkersFactoryResolver;
import org.springframework.util.Assert;

/**
 * Default implementation of {@link DatabaseClient.Builder}.
 *
 * @author Mark Paluch
 * @since 5.3
 */
class DefaultDatabaseClientBuilder implements DatabaseClient.Builder {

	@Nullable
	private BindMarkersFactory bindMarkers;

	@Nullable
	private ConnectionFactory connectionFactory;

	private ExecuteFunction executeFunction = Statement::execute;

	private boolean namedParameters = true;


	DefaultDatabaseClientBuilder() {
	}


	@Override
	public DatabaseClient.Builder bindMarkers(BindMarkersFactory bindMarkers) {
		Assert.notNull(bindMarkers, "BindMarkersFactory must not be null");
		this.bindMarkers = bindMarkers;
		return this;
	}

	@Override
	public DatabaseClient.Builder connectionFactory(ConnectionFactory factory) {
		Assert.notNull(factory, "ConnectionFactory must not be null");
		this.connectionFactory = factory;
		return this;
	}

	@Override
	public DatabaseClient.Builder executeFunction(ExecuteFunction executeFunction) {
		Assert.notNull(executeFunction, "ExecuteFunction must not be null");
		this.executeFunction = executeFunction;
		return this;
	}

	@Override
	public DatabaseClient.Builder namedParameters(boolean enabled) {
		this.namedParameters = enabled;
		return this;
	}

	@Override
	public DatabaseClient build() {
		Assert.notNull(this.connectionFactory, "ConnectionFactory must not be null");

		BindMarkersFactory bindMarkers = this.bindMarkers;

		if (bindMarkers == null) {
			if (this.namedParameters) {
				bindMarkers = BindMarkersFactoryResolver.resolve(this.connectionFactory);
			}
			else {
				bindMarkers = BindMarkersFactory.anonymous("?");
			}
		}

		return new DefaultDatabaseClient(bindMarkers, this.connectionFactory,
				this.executeFunction, this.namedParameters);
	}

	@Override
	public DatabaseClient.Builder apply(
			Consumer<DatabaseClient.Builder> builderConsumer) {
		Assert.notNull(builderConsumer, "BuilderConsumer must not be null");
		builderConsumer.accept(this);
		return this;
	}

}
