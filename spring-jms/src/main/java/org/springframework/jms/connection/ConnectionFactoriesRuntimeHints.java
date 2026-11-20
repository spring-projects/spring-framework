/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jms.connection;

import jakarta.jms.Connection;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueSession;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicSession;
import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;

/**
 * {@link RuntimeHintsRegistrar} to register hints for JMS connection factories.
 *
 * @author Brian Clozel
 * @see CachingConnectionFactory
 * @see SingleConnectionFactory
 * @see TransactionAwareConnectionFactoryProxy
 */
class ConnectionFactoriesRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
		hints.proxies().registerJdkProxy(builder -> builder.onReachableType(TypeReference.of(CachingConnectionFactory.class))
				.proxiedInterfaces(SessionProxy.class, TopicSession.class, QueueSession.class));
		hints.proxies().registerJdkProxy(builder -> builder.onReachableType(TypeReference.of(SingleConnectionFactory.class))
				.proxiedInterfaces(Connection.class, QueueConnection.class, TopicConnection.class));
		hints.proxies().registerJdkProxy(builder -> builder.onReachableType(TypeReference.of(TransactionAwareConnectionFactoryProxy.class))
				.proxiedInterfaces(Connection.class, QueueConnection.class, TopicConnection.class));
	}
}
