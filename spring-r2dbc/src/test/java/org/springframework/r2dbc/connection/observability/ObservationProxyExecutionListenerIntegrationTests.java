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
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.reporter.BuildingBlocks;
import io.micrometer.tracing.test.simple.SpansAssert;
import io.r2dbc.h2.H2ConnectionFactory;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.Result;
import org.junit.jupiter.api.BeforeEach;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.r2dbc.core.DatabaseClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;

class ObservationProxyExecutionListenerIntegrationTests extends SampleTestRunner {

	public static String CREATE_TABLE_LEGOSET = "CREATE TABLE legoset (\n" //
			+ "    id          serial CONSTRAINT id PRIMARY KEY,\n" //
			+ "    version     integer NULL,\n" //
			+ "    name        varchar(255) NOT NULL,\n" //
			+ "    manual      integer NULL\n" //
			+ ");";

	ObservationProxyExecutionListenerIntegrationTests() {
		super(SampleRunnerConfig.builder()
				.wavefrontApplicationName("spring-r2dbc-demo")
				.wavefrontServiceName("spring-r2dbc-test")
				// TODO: Add these to test against Wavefront. Remember not to commit it!
				.wavefrontToken("")
				.wavefrontUrl("")
				.build(), meterRegistry());
	}

	private ConnectionFactory connectionFactory;

	private static MeterRegistry meterRegistry() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry().withTimerObservationHandler();
		meterRegistry.observationConfig().tagsProvider(new Observation.TagsProvider<>() {
			@Override
			public boolean supportsContext(Observation.Context context) {
				return context.getName().equals(R2dbcObservation.R2DBC_QUERY_OBSERVATION.getName());
			}

			@Override
			public Tags getLowCardinalityTags(Observation.Context context) {
				return Tags.of(R2dbcObservation.LowCardinalityTags.URL.of("http://localhost:6543"));
			}

			@Override
			public Tags getHighCardinalityTags(Observation.Context context) {
				return Tags.of(Tag.of("my-high-cardinality-tag", "foo"));
			}
		});
		return meterRegistry;
	}

	@BeforeEach
	public void before() {
		connectionFactory = createConnectionFactory();

		Mono.from(connectionFactory.create())
				.flatMapMany(connection -> Flux.from(connection.createStatement("DROP TABLE legoset").execute())
						.flatMap(Result::getRowsUpdated)
						.onErrorResume(e -> Mono.empty())
						.thenMany(connection.createStatement(getCreateTableStatement()).execute())
						.flatMap(Result::getRowsUpdated).thenMany(connection.close())).as(StepVerifier::create)
				.verifyComplete();
	}


	private ConnectionFactory createConnectionFactory() {
		ConnectionFactory connectionFactory = H2ConnectionFactory.inMemory("r2dbc-observability-test");
		ProxyConnectionFactory.Builder builder = ProxyConnectionFactory.builder(connectionFactory);
		builder.listener(new ObservationProxyExecutionListener(getMeterRegistry(), connectionFactory, "my-name"));
		return builder.build();
	}

	private String getCreateTableStatement() {
		return CREATE_TABLE_LEGOSET;
	}

	@Override
	public SampleTestRunnerConsumer yourCode() throws Exception {
		return (bb, meterRegistry) -> executeInsert(bb);
	}

	private void executeInsert(BuildingBlocks bb) {
		Tracer tracer = bb.getTracer();
		Span rootSpan = tracer.currentSpan();
		DatabaseClient databaseClient = DatabaseClient.create(connectionFactory);

		databaseClient.sql("INSERT INTO legoset (id, name, manual) VALUES(:id, :name, :manual)")
				.bind("id", 42055)
				.bind("name", "SCHAUFELRADBAGGER")
				.bindNull("manual", Integer.class)
				.fetch().rowsUpdated()
				.as(StepVerifier::create)
				.expectNext(1)
				.verifyComplete();

		Span span = tracer.currentSpan();
		then(span.context().spanId()).isEqualTo(rootSpan.context().spanId());

		databaseClient.sql("SELECT id FROM legoset")
				.map(row -> row.get("id"))
				.first()
				.as(StepVerifier::create)
				.assertNext(actual -> {
					assertThat(actual).isInstanceOf(Number.class);
					assertThat(((Number) actual).intValue()).isEqualTo(42055);
				}).verifyComplete();

		span = tracer.currentSpan();
		then(span.context().spanId()).isEqualTo(rootSpan.context().spanId());

		SpansAssert.assertThat(bb.getFinishedSpans())
				.haveSameTraceId();
	}
}
