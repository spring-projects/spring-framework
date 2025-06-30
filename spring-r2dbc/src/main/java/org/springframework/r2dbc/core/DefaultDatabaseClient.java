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

package org.springframework.r2dbc.core;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Parameter;
import io.r2dbc.spi.Parameters;
import io.r2dbc.spi.R2dbcException;
import io.r2dbc.spi.Readable;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import io.r2dbc.spi.Statement;
import io.r2dbc.spi.Wrapped;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.beans.BeanUtils;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.r2dbc.connection.ConnectionFactoryUtils;
import org.springframework.r2dbc.core.binding.BindMarkersFactory;
import org.springframework.r2dbc.core.binding.BindTarget;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
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

	private final @Nullable NamedParameterExpander namedParameterExpander;


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
		return ConnectionFactoryUtils.currentConnectionFactory(obtainConnectionFactory()).then()
				.onErrorResume(Exception.class, ex -> Mono.from(connection.close()));
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
				new Class<?>[] {Connection.class, Wrapped.class},
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
	private static @Nullable String getSql(Object object) {
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

		final Map<Integer, Parameter> byIndex;

		final Map<String, Parameter> byName;

		final Supplier<String> sqlSupplier;

		final StatementFilterFunction filterFunction;

		DefaultGenericExecuteSpec(Supplier<String> sqlSupplier) {
			this.byIndex = Collections.emptyMap();
			this.byName = Collections.emptyMap();
			this.sqlSupplier = sqlSupplier;
			this.filterFunction = StatementFilterFunction.EMPTY_FILTER;
		}

		DefaultGenericExecuteSpec(Map<Integer, Parameter> byIndex, Map<String, Parameter> byName,
				Supplier<String> sqlSupplier, StatementFilterFunction filterFunction) {

			this.byIndex = byIndex;
			this.byName = byName;
			this.sqlSupplier = sqlSupplier;
			this.filterFunction = filterFunction;
		}

		@SuppressWarnings("deprecation")
		private Parameter resolveParameter(Object value) {
			if (value instanceof Parameter param) {
				return param;
			}
			else if (value instanceof org.springframework.r2dbc.core.Parameter param) {
				Object paramValue = param.getValue();
				return (paramValue != null ? Parameters.in(paramValue) : Parameters.in(param.getType()));
			}
			else {
				return Parameters.in(value);
			}
		}

		@Override
		public DefaultGenericExecuteSpec bind(int index, Object value) {
			assertNotPreparedOperation();
			Assert.notNull(value, () -> String.format(
					"Value at index %d must not be null. Use bindNull(…) instead.", index));

			Map<Integer, Parameter> byIndex = new LinkedHashMap<>(this.byIndex);
			byIndex.put(index, resolveParameter(value));

			return new DefaultGenericExecuteSpec(byIndex, this.byName, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bindNull(int index, Class<?> type) {
			assertNotPreparedOperation();

			Map<Integer, Parameter> byIndex = new LinkedHashMap<>(this.byIndex);
			byIndex.put(index, Parameters.in(type));

			return new DefaultGenericExecuteSpec(byIndex, this.byName, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bind(String name, Object value) {
			assertNotPreparedOperation();

			Assert.hasText(name, "Parameter name must not be null or empty");
			Assert.notNull(value, () -> String.format(
					"Value for parameter %s must not be null. Use bindNull(…) instead.", name));

			Map<String, Parameter> byName = new LinkedHashMap<>(this.byName);
			byName.put(name, resolveParameter(value));

			return new DefaultGenericExecuteSpec(this.byIndex, byName, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bindNull(String name, Class<?> type) {
			assertNotPreparedOperation();
			Assert.hasText(name, "Parameter name must not be null or empty");

			Map<String, Parameter> byName = new LinkedHashMap<>(this.byName);
			byName.put(name, Parameters.in(type));

			return new DefaultGenericExecuteSpec(this.byIndex, byName, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public GenericExecuteSpec bindValues(List<?> source) {
			assertNotPreparedOperation();
			Assert.notNull(source, "Source list must not be null");
			Map<Integer, Parameter> byIndex = new LinkedHashMap<>(this.byIndex);
			ListIterator<?> listIterator = source.listIterator();
			while (listIterator.hasNext()) {
				byIndex.put(listIterator.nextIndex(), resolveParameter(listIterator.next()));
			}
			return new DefaultGenericExecuteSpec(byIndex, this.byName, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public GenericExecuteSpec bindValues(Map<String, ?> source) {
			assertNotPreparedOperation();
			Assert.notNull(source, "Parameter source must not be null");

			Map<String, Parameter> target = new LinkedHashMap<>(this.byName);
			source.forEach((name, value) -> target.put(name, resolveParameter(value)));

			return new DefaultGenericExecuteSpec(this.byIndex, target, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec bindProperties(Object source) {
			assertNotPreparedOperation();
			Assert.notNull(source, "Parameter source must not be null");

			Map<String, Parameter> byName = new LinkedHashMap<>(this.byName);
			for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(source.getClass())) {
				if (pd.getReadMethod() != null && pd.getReadMethod().getDeclaringClass() != Object.class) {
					ReflectionUtils.makeAccessible(pd.getReadMethod());
					Object value = ReflectionUtils.invokeMethod(pd.getReadMethod(), source);
					byName.put(pd.getName(), (value != null ? Parameters.in(value) : Parameters.in(pd.getPropertyType())));
				}
			}

			return new DefaultGenericExecuteSpec(this.byIndex, byName, this.sqlSupplier, this.filterFunction);
		}

		@Override
		public DefaultGenericExecuteSpec filter(StatementFilterFunction filter) {
			Assert.notNull(filter, "StatementFilterFunction must not be null");
			return new DefaultGenericExecuteSpec(
					this.byIndex, this.byName, this.sqlSupplier, this.filterFunction.andThen(filter));
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
			return execute(this.sqlSupplier, result -> result.map(new DataClassRowMapper<>(mappedClass)));
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

				if (DefaultDatabaseClient.this.namedParameterExpander != null) {
					Map<String, Parameter> remainderByName = new LinkedHashMap<>(this.byName);
					Map<Integer, Parameter> remainderByIndex = new LinkedHashMap<>(this.byIndex);

					List<String> parameterNames = DefaultDatabaseClient.this.namedParameterExpander.getParameterNames(sql);
					MapBindParameterSource namedBindings = retrieveParameters(
							sql, parameterNames, remainderByName, remainderByIndex);

					PreparedOperation<String> operation = DefaultDatabaseClient.this.namedParameterExpander.expand(
							sql, DefaultDatabaseClient.this.bindMarkersFactory, namedBindings);

					String expanded = getRequiredSql(operation);
					if (logger.isTraceEnabled()) {
						logger.trace("Expanded SQL [" + expanded + "]");
					}

					Statement statement = connection.createStatement(expanded);
					BindTarget bindTarget = new StatementWrapper(statement);

					operation.bindTo(bindTarget);

					bindByName(statement, remainderByName);
					bindByIndex(statement, remainderByIndex);

					return statement;
				}

				Statement statement = connection.createStatement(sql);

				bindByIndex(statement, this.byIndex);
				bindByName(statement, this.byName);

				return statement;
			};

			return new ResultFunction(sqlSupplier, statementFunction, this.filterFunction,
					DefaultDatabaseClient.this.executeFunction);
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

		private MapBindParameterSource retrieveParameters(String sql, List<String> parameterNames,
				Map<String, Parameter> remainderByName, Map<Integer, Parameter> remainderByIndex) {

			Map<String, Parameter> namedBindings = CollectionUtils.newLinkedHashMap(parameterNames.size());
			for (String parameterName : parameterNames) {
				Parameter parameter = getParameter(remainderByName, remainderByIndex, parameterNames, parameterName);
				if (parameter == null) {
					throw new InvalidDataAccessApiUsageException(
							String.format("No parameter specified for [%s] in query [%s]", parameterName, sql));
				}
				namedBindings.put(parameterName, parameter);
			}
			return new MapBindParameterSource(namedBindings);
		}

		private @Nullable Parameter getParameter(Map<String, Parameter> remainderByName,
				Map<Integer, Parameter> remainderByIndex, List<String> parameterNames, String parameterName) {

			if (this.byName.containsKey(parameterName)) {
				remainderByName.remove(parameterName);
				return this.byName.get(parameterName);
			}

			int index = parameterNames.indexOf(parameterName);
			if (this.byIndex.containsKey(index)) {
				remainderByIndex.remove(index);
				return this.byIndex.get(index);
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
		public @Nullable Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			return switch (method.getName()) {
				// Only consider equal when proxies are identical.
				case "equals" -> proxy == args[0];
				// Use hashCode of Connection proxy.
				case "hashCode" -> System.identityHashCode(proxy);
				case "unwrap" -> this.target;
				// Handle close method: suppress, not valid.
				case "close" -> Mono.error(new UnsupportedOperationException("Close is not supported!"));
				default -> {
					try {
						// Invoke method on target Connection.
						yield method.invoke(this.target, args);
					}
					catch (InvocationTargetException ex) {
						throw ex.getTargetException();
					}
				}
			};
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
