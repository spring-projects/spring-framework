/*
 * Copyright 2013-2021 the original author or authors.
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

package org.springframework.r2dbc.connection.observability;

import io.micrometer.api.instrument.observation.Observation;
import io.micrometer.api.instrument.observation.ObservationRegistry;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.listener.ProxyExecutionListener;
import io.r2dbc.spi.ConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Observation of a {@link ProxyExecutionListener}.
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public class ObservationProxyExecutionListener implements ProxyExecutionListener {
	private static final Log logger = LogFactory.getLog(ObservationProxyExecutionListener.class);

	private final ObservationRegistry observationRegistry;

	private final ConnectionFactory connectionFactory;

	private final String connectionFactoryName;

	/**
	 * Creates an instance of {@link ObservationProxyExecutionListener}.
	 *
	 * @param observationRegistry observation registry
	 * @param connectionFactory connection factory
	 * @param connectionFactoryName connection factory name
	 */
	public ObservationProxyExecutionListener(ObservationRegistry observationRegistry, ConnectionFactory connectionFactory, String connectionFactoryName) {
		this.observationRegistry = observationRegistry;
		this.connectionFactory = connectionFactory;
		this.connectionFactoryName = connectionFactoryName;
	}

	@Override
	public void beforeQuery(QueryExecutionInfo executionInfo) {
		// TODO: How can we ensure that all of those methods are executed ONLY when context is ready?
		// we used to have issues with this getting executed extremely early

		// Ideas: ObservationRegistry available as parent context? - that implies tracing too

		//		if (isContextUnusable()) {
		//			if (log.isDebugEnabled()) {
		//				log.debug("Context is not ready - won't do anything");
		//			}
		//			return;
		//		}

		if (this.observationRegistry.getCurrentObservation() == null) {
			return;
		}
		String name = this.connectionFactory.getMetadata().getName();
		Observation observation = childObservation(executionInfo, name);
		if (logger.isDebugEnabled()) {
			logger.debug("Created a new child observation before query [" + observation + "]");
		}
		tagQueries(executionInfo, observation);
		executionInfo.getValueStore().put(Observation.Scope.class, observation.openScope());
	}

	private Observation childObservation(QueryExecutionInfo executionInfo, String name) {
		return Observation.createNotStarted(R2dbcObservation.R2DBC_QUERY_OBSERVATION.getName(), this.observationRegistry)
				.contextualName(R2dbcObservation.R2DBC_QUERY_OBSERVATION.getContextualName())
				.lowCardinalityTag(R2dbcObservation.LowCardinalityTags.BEAN_NAME.of(this.connectionFactoryName))
				.lowCardinalityTag(R2dbcObservation.LowCardinalityTags.CONNECTION.of(name))
				.lowCardinalityTag(R2dbcObservation.LowCardinalityTags.THREAD.of(executionInfo.getThreadName()))
				.start();
	}

	private void tagQueries(QueryExecutionInfo executionInfo, Observation observation) {
		int i = 0;
		for (QueryInfo queryInfo : executionInfo.getQueries()) {
			observation.highCardinalityTag(String.format(R2dbcObservation.HighCardinalityTags.QUERY.getKey(), i), queryInfo.getQuery());
			i = i + 1;
		}
	}

	@Override
	public void afterQuery(QueryExecutionInfo executionInfo) {
		// TODO: How can we ensure that all of those methods are executed ONLY when context is ready?
		// we used to have issues with this getting executed extremely early

		// Ideas: ObservationRegistry available as parent context? - that implies tracing too

		//		if (isContextUnusable()) {
		//			if (log.isDebugEnabled()) {
		//				log.debug("Context is not ready - won't do anything");
		//			}
		//			return;
		//		}
		Observation.Scope scope = executionInfo.getValueStore().get(Observation.Scope.class, Observation.Scope.class);
		if (scope == null) {
			return;
		}
		try (scope) {
			Observation observation = scope.getCurrentObservation();
			if (logger.isDebugEnabled()) {
				logger.debug("Continued the child observation in after query [" + observation + "]");
			}
			final Throwable throwable = executionInfo.getThrowable();
			if (throwable != null) {
				observation.error(throwable);
			}
			observation.stop();
		}
	}

}
