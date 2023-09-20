/*
 * Copyright 2002-2023 the original author or authors.
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Parameter;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.lang.Nullable;
import org.springframework.r2dbc.connection.ConnectionFactoryUtils;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * The default implementation of {@link DatabaseClient},
 * as created by the static factory method.
 *
 * @author Mark Paluch
 * @author Mingyuan Wu
 * @author Bogdan Ilchyshyn
 * @author Simon Baslé
 * @author Juergen Hoeller
 * @since 5.3
 * @see DatabaseClient#create(ConnectionFactory)
 */
final class DefaultDatabaseClient implements DatabaseClient {

	private final Log logger = LogFactory.getLog(getClass());

	private final BindMarkersFactory bindMarkersFactory;

	private final ConnectionFactory connectionFactory;

	private final ExecuteFunction executeFunction;

	@Nullable
	private final NamedParameterExpander namedParameterExpander;


	DefaultDatabaseClient(BindMarkersFactory bindMarkersFactory, ConnectionFactory connectionFactory,
			ExecuteFunction executeFunction, boolean namedParameters) {

		this.bindMarkersFactory = bindMarkersFactory;
		this.connectionFactory = connectionFactory;
		this.executeFunction = executeFunction;
		this.namedParameterExpander = (namedParameters ? new NamedParameterExpander() : null);
	}


	@Override
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	@Override
	public GenericExecuteSpec sql(String sql) {
		Assert.hasText(sql, "SQL must not be null or empty");
		return sql(() -> sql);
	}

	@Override
	public GenericExecuteSpec sql(Supplier<String> sqlSupplier) {
		Assert.notNull(sqlSupplier, "SQL Supplier must not be null");
		return new DefaultGenericExecuteSpec(sqlSupplier);
	}

	@Override
	public <T> Mono<T> inConnection(Function<Connection, Mono<T>> action) {
		Assert.notNull(action, "Callback object must not be null");
		Mono<ConnectionCloseHolder> connectionMono = getConnection().map(
				connection -> new ConnectionCloseHolder(connection, this::closeConnection));

		return Mono.usingWhen(connectionMono, connectionCloseHolder -> {
			// Create close-suppressing Connection proxy
			Connection connectionToUse = createConnectionProxy(connectionCloseHolder.connection);
					try {
						return action.apply(connectionToUse);
					}
					catch (R2dbcException ex) {
						String sql = getSql(action);
						return Mono.error(ConnectionFactoryUtils.convertR2dbcException("doInConnection", sql, ex));
					}
				}, ConnectionCloseHolder::close, (it, err) -> it.close(),
				ConnectionCloseHolder::close)
				.onErrorMap(R2dbcException.class,
						ex -> ConnectionFactoryUtils.convertR2dbcException("execute", getSql(action), ex));
	}

	@Override
	public <T> Flux<T> inConnectionMany(Function<Connection, Flux<T>> action) {
		Assert.notNull(action, "Callback object must not be null");
		Mono<ConnectionCloseHolder> connectionMono = getConnection().map(
				connection -> new ConnectionCloseHolder(connection, this::closeConnection));

		return Flux.usingWhen(connectionMono, connectionCloseHolder -> {
			// Create close-suppressing Connection proxy, also preparing returned Statements.
			Connection connectionToUse = createConnectionProxy(connectionCloseHolder.connection);
					try {
						return action.apply(connectionToUse);
					}
					catch (R2dbcException ex) {
						String sql = getSql(action);
						return Flux.error(ConnectionFactoryUtils.convertR2dbcException("doInConnectionMany", sql, ex));
					}
				}, ConnectionCloseHolder::close, (it, err) -> it.close(),
				ConnectionCloseHolder::close)
				.onErrorMap(R2dbcException.class,
						ex -> ConnectionFactoryUtils.convertR2dbcException("executeMany", getSql(action), ex));
	}

	/**
	 * Obtain a {@link Connection}.
	 * @return a {@link Mono} able to emit a {@link Connection}
	 */
	private Mono<Connection> getConnection() {
		return ConnectionFactoryUtils.getConnection(obtainConnectionFactory());
	}

	/**
	 * Release the {@link Connection}.
	 * @param connection to close.
	 * @return a {@link Publisher} that completes successfully when the connection is closed
	 */
	private Publisher<Void> closeConnection(Connection connection) {
		return ConnectionFactoryUtils.currentConnectionFactory(
				obtainConnectionFactory()).then().onErrorResume(Exception.class,
						e -> Mono.from(connection.close()));
	}

	/**
	 * Obtain the {@link ConnectionFactory} for actual use.
	 * @return the ConnectionFactory (never {@code null})
	 */
	private ConnectionFactory obtainConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Create a close-suppressing proxy for the given R2DBC
	 * Connection. Called by the {@code execute} method.
	 * @param con the R2DBC Connection to create a proxy for
	 * @return the Connection proxy
	 */
	private static Connection createConnectionProxy(Connection con) {
		return (Connection) Proxy.newProxyInstance(DatabaseClient.class.getClassLoader(),
				new Class<?>[] { Connection.class, Wrapped.class },
				new CloseSuppressingInvocationHandler(con));
	}

	private static Mono<Long> sumRowsUpdated(Function<Connection, Flux<Result>> resultFunction, Connection it) {
		return resultFunction.apply(it)
				.flatMap(Result::getRowsUpdated)
				.cast(Number.class)
				.collect(Collectors.summingLong(Number::longValue));
	}

	/**
	 * Get SQL from a potential provider object.
	 * @param object an object that is potentially an SqlProvider
	 * @return the SQL string, or {@code null}
	 * @see SqlProvider
	 */
	@Nullable
	private static String getSql(Object object) {
		if (object instanceof SqlProvider sqlProvider) {
			return sqlProvider.getSql();
		}
		else {
			return null;
		}
	}


	/**
	 * Default {@link DatabaseClient.GenericExecuteSpec} implementation.
	 */
	class DefaultGenericExecuteSpec implements GenericExecuteSpec {

		final Params params;

		final List<Params> batchParams;

		final Supplier<String> sqlSupplier;

		final StatementFilterFunction filterFunction;

		DefaultGenericExecuteSpec(Supplier<String> sqlSupplier) {
			this.params = DefaultParamsImpl.EMPTY_PARAMS;
			this.batchParams = Collections.emptyList();
			this.sqlSupplier = sqlSupplier;
			this.filterFunction = StatementFilterFunction.EMPTY_FILTER;
		}

		DefaultGenericExecuteSpec(Params params, List<Params> batchParams, Supplier<String> sqlSupplier,
								  StatementFilterFunction filterFunction) {
			this.params = params;
			this.batchParams = batchParams;
			this.sqlSupplier = sqlSupplier;
			this.filterFunction = filterFunction;
		}

		@Override
		public DefaultGenericExecuteSpec bind(int index, Object value) {
			assertNotPreparedOperation();

			Params newParams = this.params.bind(index, value);

			return new DefaultGenericExecuteSpec(newParams, this.batchParams, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bindNull(int index, Class<?> type) {
			assertNotPreparedOperation();

			Params newParams = this.params.bindNull(index, type);

			return new DefaultGenericExecuteSpec(newParams, this.batchParams, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bind(String name, Object value) {
			assertNotPreparedOperation();

			Params newParams = this.params.bind(name, value);

			return new DefaultGenericExecuteSpec(newParams, this.batchParams, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bindNull(String name, Class<?> type) {
			assertNotPreparedOperation();

			Params newParams = this.params.bindNull(name, type);

			return new DefaultGenericExecuteSpec(newParams, this.batchParams, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public GenericExecuteSpec bindValues(Map<String, ?> source) {
			assertNotPreparedOperation();

			Params newParams = this.params.bindValues(source);

			return new DefaultGenericExecuteSpec(newParams, this.batchParams, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bindProperties(Object source) {
			assertNotPreparedOperation();

			Params newParams = this.params.bindProperties(source);

			return new DefaultGenericExecuteSpec(newParams, this.batchParams, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public GenericExecuteSpec bind(UnaryOperator<Params> paramsFunction) {
			assertNotPreparedOperation();

			Assert.notNull(paramsFunction, "Params function must not be null");
			List<Params> newBatchParams = new ArrayList<>(this.batchParams);
			newBatchParams.add(paramsFunction.apply(DefaultParamsImpl.EMPTY_PARAMS));

			return new DefaultGenericExecuteSpec(this.params, newBatchParams, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec filter(StatementFilterFunction filter) {
			Assert.notNull(filter, "StatementFilterFunction must not be null");
			return new DefaultGenericExecuteSpec(this.params, this.batchParams, this.sqlSupplier,
					this.filterFunction.andThen(filter));
		}

		@Override
		public <R> FetchSpec<R> map(Function<? super Readable, R> mappingFunction) {
			Assert.notNull(mappingFunction, "Mapping function must not be null");
			return execute(this.sqlSupplier, result -> result.map(mappingFunction));
		}

		@Override
		public <R> FetchSpec<R> map(BiFunction<Row, RowMetadata, R> mappingFunction) {
			Assert.notNull(mappingFunction, "Mapping function must not be null");
			return execute(this.sqlSupplier, result -> result.map(mappingFunction));
		}

		@Override
		public <R> RowsFetchSpec<R> mapValue(Class<R> mappedClass) {
			Assert.notNull(mappedClass, "Mapped class must not be null");
			return execute(this.sqlSupplier, result -> result.map(row -> row.get(0, mappedClass)));
		}

		@Override
		public <R> FetchSpec<R> mapProperties(Class<R> mappedClass) {
			Assert.notNull(mappedClass, "Mapped class must not be null");
			return execute(this.sqlSupplier, result -> result.map(new DataClassRowMapper<R>(mappedClass)));
		}

		@Override
		public <R> Flux<R> flatMap(Function<Result, Publisher<R>> mappingFunction) {
			Assert.notNull(mappingFunction, "Mapping function must not be null");
			return flatMap(this.sqlSupplier, mappingFunction);
		}

		@Override
		public FetchSpec<Map<String, Object>> fetch() {
			return execute(this.sqlSupplier, result -> result.map(ColumnMapRowMapper.INSTANCE));
		}

		@Override
		public Mono<Void> then() {
			return fetch().rowsUpdated().then();
		}

		private ResultFunction getResultFunction(Supplier<String> sqlSupplier) {
			BiFunction<Connection, String, Statement> statementFunction = (connection, sql) -> {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing SQL statement [" + sql + "]");
				}
				if (sqlSupplier instanceof PreparedOperation<?> preparedOperation) {
					Statement statement = connection.createStatement(sql);
					BindTarget bindTarget = new StatementWrapper(statement);
					preparedOperation.bindTo(bindTarget);
					return statement;
				}

				List<Params> allParams = new ArrayList<>(this.batchParams);

				if (!this.params.byIndex().isEmpty() || !this.params.byName().isEmpty()) {
					allParams.add(this.params);
				}

				if (!allParams.isEmpty() && DefaultDatabaseClient.this.namedParameterExpander != null) {
					List<String> parameterNames = DefaultDatabaseClient.this.namedParameterExpander.getParameterNames(sql);
					List<PreparedOperation<String>> operations = new ArrayList<>(allParams.size());

					for (Params params : allParams) {
						MapBindParameterSource namedBindings = retrieveParameters(sql, parameterNames, params);

					PreparedOperation<String> operation = DefaultDatabaseClient.this.namedParameterExpander.expand(
							sql, DefaultDatabaseClient.this.bindMarkersFactory, namedBindings);

						operations.add(operation);
					}

					String expanded = getRequiredSql(operations);
					if (logger.isTraceEnabled()) {
						logger.trace("Expanded SQL [" + expanded + "]");
					}

					Statement statement = connection.createStatement(expanded);
					BindTarget bindTarget = new StatementWrapper(statement);

					for (int i = 0; i < operations.size(); i++) {
						PreparedOperation<String> operation = operations.get(i);
						operation.bindTo(bindTarget);
						if (operations.size() > 1 && i != operations.size() - 1) {
							statement.add();
						}
					}

					applyBindings(statement, allParams);

					return statement;
				}

				Statement statement = connection.createStatement(sql);

				applyBindings(statement, allParams);

				return statement;
			};

			return new ResultFunction(sqlSupplier, statementFunction, this.filterFunction,
					DefaultDatabaseClient.this.executeFunction);
		}

		private void applyBindings(Statement statement, List<Params> params) {
			for (int i = 0; i < params.size(); i++) {
				Params parameter = params.get(i);
				if (!parameter.byIndex().isEmpty() || !parameter.byName().isEmpty()) {
					bindByIndex(statement, parameter.byIndex());
					bindByName(statement, parameter.byName());
					if (params.size() > 1 && i != params.size() - 1) {
						statement.add();
					}
				}
			}
		}

		private <T> FetchSpec<T> execute(Supplier<String> sqlSupplier, Function<Result, Publisher<T>> resultAdapter) {
			ResultFunction resultHandler = getResultFunction(sqlSupplier);
			return new DefaultFetchSpec<>(DefaultDatabaseClient.this, resultHandler,
					connection -> sumRowsUpdated(resultHandler, connection), resultAdapter);
		}

		private <T> Flux<T> flatMap(Supplier<String> sqlSupplier, Function<Result, Publisher<T>> mappingFunction) {
			ResultFunction resultHandler = getResultFunction(sqlSupplier);
			ConnectionFunction<Flux<T>> connectionFunction = new DelegateConnectionFunction<>(resultHandler,
					cx -> resultHandler.apply(cx).flatMap(mappingFunction));
			return inConnectionMany(connectionFunction);
		}

		private MapBindParameterSource retrieveParameters(String sql, List<String> parameterNames, Params params) {

			Map<String, Parameter> namedBindings = CollectionUtils.newLinkedHashMap(parameterNames.size());
			for (String parameterName : parameterNames) {
				Parameter parameter = getParameter(params.byName(), params.byIndex(), parameterNames, parameterName);
				if (parameter == null) {
					throw new InvalidDataAccessApiUsageException(
							String.format("No parameter specified for [%s] in query [%s]", parameterName, sql));
				}
				namedBindings.put(parameterName, parameter);
			}
			return new MapBindParameterSource(namedBindings);
		}

		@Nullable
		private Parameter getParameter(Map<String, Parameter> byName, Map<Integer, Parameter> byIndex,
									   List<String> parameterNames, String parameterName) {
			if (byName.containsKey(parameterName)) {
				return byName.remove(parameterName);
			}

			int index = parameterNames.indexOf(parameterName);
			if (byIndex.containsKey(index)) {
				return byIndex.remove(index);
			}

			return null;
		}

		private void assertNotPreparedOperation() {
			if (this.sqlSupplier instanceof PreparedOperation<?>) {
				throw new InvalidDataAccessApiUsageException("Cannot add bindings to a PreparedOperation");
			}
		}

		private void bindByName(Statement statement, Map<String, Parameter> byName) {
			byName.forEach(statement::bind);
		}

		private void bindByIndex(Statement statement, Map<Integer, Parameter> byIndex) {
			byIndex.forEach(statement::bind);
		}

		private String getRequiredSql(List<PreparedOperation<String>> operations) throws IllegalArgumentException {
			return operations
					.stream()
					.map(this::getRequiredSql)
					.reduce((prevSql, nextSql) -> {
						if (prevSql.equals(nextSql)) {
							return nextSql;
						} else {
							throw new IllegalArgumentException("Resulting SQL is not the same!");
						}
					}).orElseThrow(() -> new IllegalArgumentException("Operations must not be empty!"));
		}

		private String getRequiredSql(Supplier<String> sqlSupplier) {
			String sql = sqlSupplier.get();
			Assert.state(StringUtils.hasText(sql), "SQL returned by supplier must not be empty");
			return sql;
		}
	}


	/**
	 * Invocation handler that suppresses close calls on R2DBC Connections. Also prepares
	 * returned Statement (Prepared/CallbackStatement) objects.
	 *
	 * @see Connection#close()
	 */
	private static class CloseSuppressingInvocationHandler implements InvocationHandler {

		private final Connection target;

		CloseSuppressingInvocationHandler(Connection target) {
			this.target = target;
		}

		@Override
		@Nullable
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			switch (method.getName()) {
				case "equals":
					// Only consider equal when proxies are identical.
					return proxy == args[0];
				case "hashCode":
					// Use hashCode of PersistenceManager proxy.
					return System.identityHashCode(proxy);
				case "unwrap":
					return this.target;
				case "close":
					// Handle close method: suppress, not valid.
					return Mono.error(new UnsupportedOperationException("Close is not supported!"));
			}

			// Invoke method on target Connection.
			try {
				return method.invoke(this.target, args);
			}
			catch (InvocationTargetException ex) {
				throw ex.getTargetException();
			}
		}
	}


	/**
	 * Holder for a connection that makes sure the close action is invoked atomically only once.
	 */
	static class ConnectionCloseHolder extends AtomicBoolean {

		private static final long serialVersionUID = -8994138383301201380L;

		final transient Connection connection;

		final transient Function<Connection, Publisher<Void>> closeFunction;

		ConnectionCloseHolder(Connection connection, Function<Connection, Publisher<Void>> closeFunction) {
			this.connection = connection;
			this.closeFunction = closeFunction;
		}

		Mono<Void> close() {
			return Mono.defer(() -> {
				if (compareAndSet(false, true)) {
					return Mono.from(this.closeFunction.apply(this.connection));
				}
				return Mono.empty();
			});
		}
	}


	static class StatementWrapper implements BindTarget {

		final Statement statement;

		StatementWrapper(Statement statement) {
			this.statement = statement;
		}

		@Override
		public void bind(String identifier, Object value) {
			this.statement.bind(identifier, value);
		}

		@Override
		public void bind(int index, Object value) {
			this.statement.bind(index, value);
		}

		@Override
		public void bindNull(String identifier, Class<?> type) {
			this.statement.bindNull(identifier, type);
		}

		@Override
		public void bindNull(int index, Class<?> type) {
			this.statement.bindNull(index, type);
		}
	}

}
