/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jms.connection;

import jakarta.jms.Connection;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueSession;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicSession;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.Nullable;

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
