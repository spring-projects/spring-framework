/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jca.cci.core;

import java.sql.SQLException;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResultSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jca.cci.CannotCreateRecordException;
import org.springframework.jca.cci.CciOperationNotSupportedException;
import org.springframework.jca.cci.InvalidResultSetAccessException;
import org.springframework.jca.cci.RecordTypeNotSupportedException;
import org.springframework.jca.cci.connection.ConnectionFactoryUtils;
import org.springframework.jca.cci.connection.NotSupportedRecordFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * <b>This is the central class in the CCI core package.</b>
 * It simplifies the use of CCI and helps to avoid common errors.
 * It executes core CCI workflow, leaving application code to provide parameters
 * to CCI and extract results. This class executes EIS queries or updates,
 * catching ResourceExceptions and translating them to the generic exception
 * hierarchy defined in the {@code org.springframework.dao} package.
 *
 * <p>Code using this class can pass in and receive {@link javax.resource.cci.Record}
 * instances, or alternatively implement callback interfaces for creating input
 * Records and extracting result objects from output Records (or CCI ResultSets).
 *
 * <p>Can be used within a service implementation via direct instantiation
 * with a ConnectionFactory reference, or get prepared in an application context
 * and given to services as bean reference. Note: The ConnectionFactory should
 * always be configured as a bean in the application context, in the first case
 * given to the service directly, in the second case to the prepared template.
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see RecordCreator
 * @see RecordExtractor
 */
public class CciTemplate implements CciOperations {

	private final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private ConnectionFactory connectionFactory;

	@Nullable
	private ConnectionSpec connectionSpec;

	@Nullable
	private RecordCreator outputRecordCreator;


	/**
	 * Construct a new CciTemplate for bean usage.
	 * <p>Note: The ConnectionFactory has to be set before using the instance.
	 * @see #setConnectionFactory
	 */
	public CciTemplate() {
	}

	/**
	 * Construct a new CciTemplate, given a ConnectionFactory to obtain Connections from.
	 * Note: This will trigger eager initialization of the exception translator.
	 * @param connectionFactory the JCA ConnectionFactory to obtain Connections from
	 */
	public CciTemplate(ConnectionFactory connectionFactory) {
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}

	/**
	 * Construct a new CciTemplate, given a ConnectionFactory to obtain Connections from.
	 * Note: This will trigger eager initialization of the exception translator.
	 * @param connectionFactory the JCA ConnectionFactory to obtain Connections from
	 * @param connectionSpec the CCI ConnectionSpec to obtain Connections for
	 * (may be {@code null})
	 */
	public CciTemplate(ConnectionFactory connectionFactory, @Nullable ConnectionSpec connectionSpec) {
		setConnectionFactory(connectionFactory);
		if (connectionSpec != null) {
			setConnectionSpec(connectionSpec);
		}
		afterPropertiesSet();
	}


	/**
	 * Set the CCI ConnectionFactory to obtain Connections from.
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Return the CCI ConnectionFactory used by this template.
	 */
	@Nullable
	public ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	private ConnectionFactory obtainConnectionFactory() {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory;
	}

	/**
	 * Set the CCI ConnectionSpec that this template instance is
	 * supposed to obtain Connections for.
	 */
	public void setConnectionSpec(@Nullable ConnectionSpec connectionSpec) {
		this.connectionSpec = connectionSpec;
	}

	/**
	 * Return the CCI ConnectionSpec used by this template, if any.
	 */
	@Nullable
	public ConnectionSpec getConnectionSpec() {
		return this.connectionSpec;
	}

	/**
	 * Set a RecordCreator that should be used for creating default output Records.
	 * <p>Default is none: When no explicit output Record gets passed into an
	 * {@code execute} method, CCI's {@code Interaction.execute} variant
	 * that returns an output Record will be called.
	 * <p>Specify a RecordCreator here if you always need to call CCI's
	 * {@code Interaction.execute} variant with a passed-in output Record.
	 * Unless there is an explicitly specified output Record, CciTemplate will
	 * then invoke this RecordCreator to create a default output Record instance.
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record)
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record, Record)
	 */
	public void setOutputRecordCreator(@Nullable RecordCreator creator) {
		this.outputRecordCreator = creator;
	}

	/**
	 * Return a RecordCreator that should be used for creating default output Records.
	 */
	@Nullable
	public RecordCreator getOutputRecordCreator() {
		return this.outputRecordCreator;
	}

	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
	}


	/**
	 * Create a template derived from this template instance,
	 * inheriting the ConnectionFactory and other settings but
	 * overriding the ConnectionSpec used for obtaining Connections.
	 * @param connectionSpec the CCI ConnectionSpec that the derived template
	 * instance is supposed to obtain Connections for
	 * @return the derived template instance
	 * @see #setConnectionSpec
	 */
	public CciTemplate getDerivedTemplate(ConnectionSpec connectionSpec) {
		CciTemplate derived = new CciTemplate(obtainConnectionFactory(), connectionSpec);
		RecordCreator recordCreator = getOutputRecordCreator();
		if (recordCreator != null) {
			derived.setOutputRecordCreator(recordCreator);
		}
		return derived;
	}


	@Override
	@Nullable
	public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		ConnectionFactory connectionFactory = obtainConnectionFactory();
		Connection con = ConnectionFactoryUtils.getConnection(connectionFactory, getConnectionSpec());
		try {
			return action.doInConnection(con, connectionFactory);
		}
		catch (NotSupportedException ex) {
			throw new CciOperationNotSupportedException("CCI operation not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new DataAccessResourceFailureException("CCI operation failed", ex);
		}
		catch (SQLException ex) {
			throw new InvalidResultSetAccessException("Parsing of CCI ResultSet failed", ex);
		}
		finally {
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory());
		}
	}

	@Override
	@Nullable
	public <T> T execute(final InteractionCallback<T> action) throws DataAccessException {
		Assert.notNull(action, "Callback object must not be null");
		return execute((ConnectionCallback<T>) (connection, connectionFactory) -> {
			Interaction interaction = connection.createInteraction();
			try {
				return action.doInInteraction(interaction, connectionFactory);
			}
			finally {
				closeInteraction(interaction);
			}
		});
	}

	@Override
	@Nullable
	public Record execute(InteractionSpec spec, Record inputRecord) throws DataAccessException {
		return doExecute(spec, inputRecord, null, new SimpleRecordExtractor());
	}

	@Override
	public void execute(InteractionSpec spec, Record inputRecord, Record outputRecord) throws DataAccessException {
		doExecute(spec, inputRecord, outputRecord, null);
	}

	@Override
	public Record execute(InteractionSpec spec, RecordCreator inputCreator) throws DataAccessException {
		Record output = doExecute(spec, createRecord(inputCreator), null, new SimpleRecordExtractor());
		Assert.state(output != null, "Invalid output record");
		return output;
	}

	@Override
	public <T> T execute(InteractionSpec spec, Record inputRecord, RecordExtractor<T> outputExtractor)
			throws DataAccessException {

		return doExecute(spec, inputRecord, null, outputExtractor);
	}

	@Override
	public <T> T execute(InteractionSpec spec, RecordCreator inputCreator, RecordExtractor<T> outputExtractor)
			throws DataAccessException {

		return doExecute(spec, createRecord(inputCreator), null, outputExtractor);
	}

	/**
	 * Execute the specified interaction on an EIS with CCI.
	 * All other interaction execution methods go through this.
	 * @param spec the CCI InteractionSpec instance that defines
	 * the interaction (connector-specific)
	 * @param inputRecord the input record
	 * @param outputRecord output record (can be {@code null})
	 * @param outputExtractor object to convert the output record to a result object
	 * @return the output data extracted with the RecordExtractor object
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	protected <T> T doExecute(
			final InteractionSpec spec, final Record inputRecord, @Nullable final Record outputRecord,
			@Nullable final RecordExtractor<T> outputExtractor) throws DataAccessException {

		return execute((InteractionCallback<T>) (interaction, connectionFactory) -> {
			Record outputRecordToUse = outputRecord;
			try {
				if (outputRecord != null || getOutputRecordCreator() != null) {
					// Use the CCI execute method with output record as parameter.
					if (outputRecord == null) {
						RecordFactory recordFactory = getRecordFactory(connectionFactory);
						outputRecordToUse = getOutputRecordCreator().createRecord(recordFactory);
					}
					interaction.execute(spec, inputRecord, outputRecordToUse);
				}
				else {
					outputRecordToUse = interaction.execute(spec, inputRecord);
				}
				return (outputExtractor != null ? outputExtractor.extractData(outputRecordToUse) : null);
			}
			finally {
				if (outputRecordToUse instanceof ResultSet) {
					closeResultSet((ResultSet) outputRecordToUse);
				}
			}
		});
	}


	/**
	 * Create an indexed Record through the ConnectionFactory's RecordFactory.
	 * @param name the name of the record
	 * @return the Record
	 * @throws DataAccessException if creation of the Record failed
	 * @see #getRecordFactory(javax.resource.cci.ConnectionFactory)
	 * @see javax.resource.cci.RecordFactory#createIndexedRecord(String)
	 */
	public IndexedRecord createIndexedRecord(String name) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(obtainConnectionFactory());
			return recordFactory.createIndexedRecord(name);
		}
		catch (NotSupportedException ex) {
			throw new RecordTypeNotSupportedException("Creation of indexed Record not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new CannotCreateRecordException("Creation of indexed Record failed", ex);
		}
	}

	/**
	 * Create a mapped Record from the ConnectionFactory's RecordFactory.
	 * @param name record name
	 * @return the Record
	 * @throws DataAccessException if creation of the Record failed
	 * @see #getRecordFactory(javax.resource.cci.ConnectionFactory)
	 * @see javax.resource.cci.RecordFactory#createMappedRecord(String)
	 */
	public MappedRecord createMappedRecord(String name) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(obtainConnectionFactory());
			return recordFactory.createMappedRecord(name);
		}
		catch (NotSupportedException ex) {
			throw new RecordTypeNotSupportedException("Creation of mapped Record not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new CannotCreateRecordException("Creation of mapped Record failed", ex);
		}
	}

	/**
	 * Invoke the given RecordCreator, converting JCA ResourceExceptions
	 * to Spring's DataAccessException hierarchy.
	 * @param recordCreator the RecordCreator to invoke
	 * @return the created Record
	 * @throws DataAccessException if creation of the Record failed
	 * @see #getRecordFactory(javax.resource.cci.ConnectionFactory)
	 * @see RecordCreator#createRecord(javax.resource.cci.RecordFactory)
	 */
	protected Record createRecord(RecordCreator recordCreator) throws DataAccessException {
		try {
			RecordFactory recordFactory = getRecordFactory(obtainConnectionFactory());
			return recordCreator.createRecord(recordFactory);
		}
		catch (NotSupportedException ex) {
			throw new RecordTypeNotSupportedException(
					"Creation of the desired Record type not supported by connector", ex);
		}
		catch (ResourceException ex) {
			throw new CannotCreateRecordException("Creation of the desired Record failed", ex);
		}
	}

	/**
	 * Return a RecordFactory for the given ConnectionFactory.
	 * <p>Default implementation returns the connector's RecordFactory if
	 * available, falling back to a NotSupportedRecordFactory placeholder.
	 * This allows to invoke a RecordCreator callback with a non-null
	 * RecordFactory reference in any case.
	 * @param connectionFactory the CCI ConnectionFactory
	 * @return the CCI RecordFactory for the ConnectionFactory
	 * @throws ResourceException if thrown by CCI methods
	 * @see org.springframework.jca.cci.connection.NotSupportedRecordFactory
	 */
	protected RecordFactory getRecordFactory(ConnectionFactory connectionFactory) throws ResourceException {
		try {
			return connectionFactory.getRecordFactory();
		}
		catch (NotSupportedException ex) {
			return new NotSupportedRecordFactory();
		}
	}


	/**
	 * Close the given CCI Interaction and ignore any thrown exception.
	 * This is useful for typical finally blocks in manual CCI code.
	 * @param interaction the CCI Interaction to close
	 * @see javax.resource.cci.Interaction#close()
	 */
	private void closeInteraction(@Nullable Interaction interaction) {
		if (interaction != null) {
			try {
				interaction.close();
			}
			catch (ResourceException ex) {
				logger.trace("Could not close CCI Interaction", ex);
			}
			catch (Throwable ex) {
				// We don't trust the CCI driver: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing CCI Interaction", ex);
			}
		}
	}

	/**
	 * Close the given CCI ResultSet and ignore any thrown exception.
	 * This is useful for typical finally blocks in manual CCI code.
	 * @param resultSet the CCI ResultSet to close
	 * @see javax.resource.cci.ResultSet#close()
	 */
	private void closeResultSet(@Nullable ResultSet resultSet) {
		if (resultSet != null) {
			try {
				resultSet.close();
			}
			catch (SQLException ex) {
				logger.trace("Could not close CCI ResultSet", ex);
			}
			catch (Throwable ex) {
				// We don't trust the CCI driver: It might throw RuntimeException or Error.
				logger.trace("Unexpected exception on closing CCI ResultSet", ex);
			}
		}
	}


	private static class SimpleRecordExtractor implements RecordExtractor<Record> {

		@Override
		public Record extractData(Record record) {
			return record;
		}
	}

}
