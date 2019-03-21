/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jca.cci.connection;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResourceAdapterMetaData;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * CCI {@link ConnectionFactory} implementation that delegates all calls
 * to a given target {@link ConnectionFactory}.
 *
 * <p>This class is meant to be subclassed, with subclasses overriding only
 * those methods (such as {@link #getConnection()}) that should not simply
 * delegate to the target {@link ConnectionFactory}.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see #getConnection
 */
@SuppressWarnings("serial")
public class DelegatingConnectionFactory implements ConnectionFactory, InitializingBean {

	@Nullable
	private ConnectionFactory targetConnectionFactory;


	/**
	 * Set the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	public void setTargetConnectionFactory(@Nullable ConnectionFactory targetConnectionFactory) {
		this.targetConnectionFactory = targetConnectionFactory;
	}

	/**
	 * Return the target ConnectionFactory that this ConnectionFactory should delegate to.
	 */
	@Nullable
	public ConnectionFactory getTargetConnectionFactory() {
		return this.targetConnectionFactory;
	}

	/**
	 * Obtain the target {@code ConnectionFactory} for actual use (never {@code null}).
	 * @since 5.0
	 */
	protected ConnectionFactory obtainTargetConnectionFactory() {
		ConnectionFactory connectionFactory = getTargetConnectionFactory();
		Assert.state(connectionFactory != null, "No 'targetConnectionFactory' set");
		return connectionFactory;
	}


	@Override
	public void afterPropertiesSet() {
		if (getTargetConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'targetConnectionFactory' is required");
		}
	}


	@Override
	public Connection getConnection() throws ResourceException {
		return obtainTargetConnectionFactory().getConnection();
	}

	@Override
	public Connection getConnection(ConnectionSpec connectionSpec) throws ResourceException {
		return obtainTargetConnectionFactory().getConnection(connectionSpec);
	}

	@Override
	public RecordFactory getRecordFactory() throws ResourceException {
		return obtainTargetConnectionFactory().getRecordFactory();
	}

	@Override
	public ResourceAdapterMetaData getMetaData() throws ResourceException {
		return obtainTargetConnectionFactory().getMetaData();
	}

	@Override
	public Reference getReference() throws NamingException {
		return obtainTargetConnectionFactory().getReference();
	}

	@Override
	public void setReference(Reference reference) {
		obtainTargetConnectionFactory().setReference(reference);
	}

}
