/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.connection.lookup;

import io.r2dbc.spi.ConnectionFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.util.Assert;

/**
 * {@link ConnectionFactoryLookup} implementation based on a
 * Spring {@link BeanFactory}.
 *
 * <p>Will lookup Spring managed beans identified by bean name,
 * expecting them to be of type {@link ConnectionFactory}.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BeanFactory
 */
public class BeanFactoryConnectionFactoryLookup implements ConnectionFactoryLookup, BeanFactoryAware {

	private @Nullable BeanFactory beanFactory;


	/**
	 * Create a new instance of the {@link BeanFactoryConnectionFactoryLookup} class.
	 * <p>The BeanFactory to access must be set via {@code setBeanFactory}.
	 * @see #setBeanFactory
	 */
	public BeanFactoryConnectionFactoryLookup() {}

	/**
	 * Create a new instance of the {@link BeanFactoryConnectionFactoryLookup} class.
	 * <p>Use of this constructor is redundant if this object is being created
	 * by a Spring IoC container, as the supplied {@link BeanFactory} will be
	 * replaced by the {@link BeanFactory} that creates it (c.f. the
	 * {@link BeanFactoryAware} contract). So only use this constructor if you
	 * are using this class outside the context of a Spring IoC container.
	 * @param beanFactory the bean factory to be used to lookup {@link ConnectionFactory
	 * ConnectionFactories}
	 */
	public BeanFactoryConnectionFactoryLookup(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public ConnectionFactory getConnectionFactory(String connectionFactoryName)
			throws ConnectionFactoryLookupFailureException {

		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(connectionFactoryName, ConnectionFactory.class);
		}
		catch (BeansException ex) {
			throw new ConnectionFactoryLookupFailureException(
					String.format("Failed to look up ConnectionFactory bean with name '%s'", connectionFactoryName), ex);
		}
	}

}
