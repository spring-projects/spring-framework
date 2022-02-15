/*
 * Copyright 2019-2020 the original author or authors.
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

import io.micrometer.api.instrument.MeterRegistry;
import io.micrometer.api.instrument.Tag;
import io.micrometer.api.instrument.Tags;
import io.micrometer.api.instrument.observation.Observation;
import io.micrometer.api.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.proxy.core.QueryExecutionInfo;
import io.r2dbc.proxy.core.QueryInfo;
import io.r2dbc.proxy.test.MockQueryExecutionInfo;
import org.junit.jupiter.api.Test;

import org.springframework.r2dbc.connection.SingleConnectionFactory;

import static io.micrometer.core.tck.MeterRegistryAssert.then;

class ObservationProxyExecutionListenerMetricsUnitTests {

	@Test
	void shouldNotCreateObservationsForR2dbcWhenThereIsNoObservation() {
		MeterRegistry registry = registry();
		SingleConnectionFactory factory = singleConnectionFactory();
		ObservationProxyExecutionListener listener = new ObservationProxyExecutionListener(registry, factory, "hello");

		listener.beforeQuery(MockQueryExecutionInfo.builder().build());
		listener.afterQuery(MockQueryExecutionInfo.builder().build());

		thenNothingHappened(registry);
	}

	void thenNothingHappened(MeterRegistry registry) {
		then(registry).hasNoMetrics();
	}

	@Test
	void shouldCreateObservationsForR2dbc() {
		MeterRegistry registry = registry();
		SingleConnectionFactory factory = singleConnectionFactory();
		QueryExecutionInfo queryExecutionInfo = queryExecutionInfo().build();
		ObservationProxyExecutionListener listener = listener(registry, factory);

		runBeforeAndAfterQuery(registry, listener, queryExecutionInfo);

		thenObservabilityGotApplied(registry);
	}

	void thenObservabilityGotApplied(MeterRegistry registry) {
		then(registry).hasTimerWithNameAndTags(R2dbcObservation.R2DBC_QUERY_OBSERVATION.getName(), Tags.of(
				Tag.of("additional.tag", "bar"),
				R2dbcObservation.LowCardinalityTags.BEAN_NAME.of("hello"),
				R2dbcObservation.LowCardinalityTags.CONNECTION.of("H2"),
				R2dbcObservation.LowCardinalityTags.THREAD.of("test-thread"),
				R2dbcObservation.LowCardinalityTags.URL.of("http://localhost:6543"),
				Tag.of("error", "none")
		));
	}

	@Test
	void shouldCreateObservationsForR2dbcWithError() {
		MeterRegistry registry = registry();
		SingleConnectionFactory factory = singleConnectionFactory();
		QueryExecutionInfo queryExecutionInfo = queryExecutionInfo().throwable(new RuntimeException("Boom!")).build();
		ObservationProxyExecutionListener listener = listener(registry, factory);

		runBeforeAndAfterQuery(registry, listener, queryExecutionInfo);

		thenObservabilityGotAppliedWithException(registry);
	}

	void thenObservabilityGotAppliedWithException(MeterRegistry registry) {
		then(registry).hasTimerWithNameAndTags(R2dbcObservation.R2DBC_QUERY_OBSERVATION.getName(), Tags.of(
				Tag.of("additional.tag", "bar"),
				R2dbcObservation.LowCardinalityTags.BEAN_NAME.of("hello"),
				R2dbcObservation.LowCardinalityTags.CONNECTION.of("H2"),
				R2dbcObservation.LowCardinalityTags.THREAD.of("test-thread"),
				R2dbcObservation.LowCardinalityTags.URL.of("http://localhost:6543"),
				Tag.of("error", "Boom!")
		));
	}

	private SingleConnectionFactory singleConnectionFactory() {
		return new SingleConnectionFactory("r2dbc:h2:mem:///foo", false);
	}

	private void runBeforeAndAfterQuery(MeterRegistry registry, ObservationProxyExecutionListener listener, QueryExecutionInfo queryExecutionInfo) {
		Observation.createNotStarted("test", registry).scoped(() -> {
			listener.beforeQuery(queryExecutionInfo);
			assertStateAfterBeforeQuery();
			listener.afterQuery(queryExecutionInfo);
			assertStateAfterAfterQuery();
		});
	}

	void assertStateAfterBeforeQuery() {

	}

	void assertStateAfterAfterQuery() {

	}

	private MockQueryExecutionInfo.Builder queryExecutionInfo() {
		return MockQueryExecutionInfo.builder()
				.threadName("test-thread")
				.queryInfo(new QueryInfo("foo"));
	}

	private ObservationProxyExecutionListener listener(MeterRegistry registry, SingleConnectionFactory factory) {
		return new ObservationProxyExecutionListener(registry, factory, "hello");
	}

	MeterRegistry registry() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry().withTimerObservationHandler();
		meterRegistry.observationConfig().tagsProvider(new Observation.TagsProvider<>() {
			@Override
			public boolean supportsContext(Observation.Context context) {
				return true;
			}

			@Override
			public Tags getLowCardinalityTags(Observation.Context context) {
				return Tags.of(Tag.of("additional.tag", "bar"), R2dbcObservation.LowCardinalityTags.URL.of("http://localhost:6543"));
			}
		});
		return meterRegistry;
	}
}
