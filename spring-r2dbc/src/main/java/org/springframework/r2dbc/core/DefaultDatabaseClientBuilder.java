/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import java.util.function.Consumer;

import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Statement;
import org.jspecify.annotations.Nullable;

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

	private @Nullable BindMarkersFactory bindMarkers;

	private @Nullable ConnectionFactory connectionFactory;

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

		return new DefaultDatabaseClient(
				bindMarkers, this.connectionFactory, this.executeFunction, this.namedParameters);
	}

	@Override
	public DatabaseClient.Builder apply(Consumer<DatabaseClient.Builder> builderConsumer) {
		Assert.notNull(builderConsumer, "BuilderConsumer must not be null");
		builderConsumer.accept(this);
		return this;
	}

}
