/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.test.context.transaction.reactive;

import java.util.Map;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.test.context.TestContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

/**
 * Utility methods for working with transactions and data access related beans
 * within the <em>Spring TestContext Framework</em>.
 *
 * <p>Mainly for internal use within the framework.
 *
 * @author jonghoon park
 * @since 7.0
 */
public abstract class TestContextReactiveTransactionUtils {

	/**
	 * Default bean name for a {@link ConnectionFactory}:
	 * {@code "connectionFactory"}.
	 */
	public static final String DEFAULT_CONNECTION_FACTORY_NAME = "connectionFactory";


	private static final Log logger = LogFactory.getLog(TestContextReactiveTransactionUtils.class);

	/**
	 * Retrieve the {@link ConnectionFactory} to use for the supplied {@linkplain TestContext
	 * test context}.
	 * <p>The following algorithm is used to retrieve the {@code ConnectionFactory} from
	 * the {@link org.springframework.context.ApplicationContext ApplicationContext}
	 * of the supplied test context:
	 * <ol>
	 * <li>Attempt to look up the single {@code ConnectionFactory} by type.
	 * <li>Attempt to look up the <em>primary</em> {@code ConnectionFactory} by type.
	 * <li>Attempt to look up the {@code ConnectionFactory} by type and the
	 * {@linkplain #DEFAULT_CONNECTION_FACTORY_NAME default data source name}.
	 * </ol>
	 * @param testContext the test context for which the {@code ConnectionFactory}
	 * should be retrieved; never {@code null}
	 * @return the {@code DataSource} to use, or {@code null} if not found
	 */
	@Nullable
	public static ConnectionFactory retrieveConnectionFactory(TestContext testContext) {
		Assert.notNull(testContext, "TestContext must not be null");
		BeanFactory bf = testContext.getApplicationContext().getAutowireCapableBeanFactory();

		try {
			if (bf instanceof ListableBeanFactory lbf) {
				// Look up single bean by type
				Map<String, ConnectionFactory> ConnectionFactories =
						BeanFactoryUtils.beansOfTypeIncludingAncestors(lbf, ConnectionFactory.class);
				if (ConnectionFactories.size() == 1) {
					return ConnectionFactories.values().iterator().next();
				}

				try {
					// look up single bean by type, with support for 'primary' beans
					return bf.getBean(ConnectionFactory.class);
				}
				catch (BeansException ex) {
					logBeansException(testContext, ex, PlatformTransactionManager.class);
				}
			}

			// look up by type and default name
			return bf.getBean(DEFAULT_CONNECTION_FACTORY_NAME, ConnectionFactory.class);
		}
		catch (BeansException ex) {
			logBeansException(testContext, ex, Connection.class);
			return null;
		}
	}

	private static void logBeansException(TestContext testContext, BeansException ex, Class<?> beanType) {
		if (logger.isTraceEnabled()) {
			logger.trace("Caught exception while retrieving %s for test context %s"
					.formatted(beanType.getSimpleName(), testContext), ex);
		}
	}
}
