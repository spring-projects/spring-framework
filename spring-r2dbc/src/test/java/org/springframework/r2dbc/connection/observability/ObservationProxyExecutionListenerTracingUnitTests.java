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
import io.micrometer.tracing.Span;
import io.micrometer.tracing.test.simple.SimpleTracer;
import io.micrometer.tracing.test.simple.SpanAssert;

import static org.assertj.core.api.BDDAssertions.then;

class ObservationProxyExecutionListenerTracingUnitTests extends ObservationProxyExecutionListenerMetricsUnitTests {

	SimpleTracer tracer = new SimpleTracer();

	@Override
	void thenNothingHappened(MeterRegistry registry) {
		then(tracer.getSpans()).isEmpty();
	}

	@Override
	void thenObservabilityGotApplied(MeterRegistry registry) {
		thenASingleSpanSet();
	}

	@Override
	void assertStateAfterBeforeQuery() {
		then(tracer.currentSpan()).isNotNull();
	}
	@Override
	void assertStateAfterAfterQuery() {
		then(tracer.currentSpan()).isNull();
	}

	private SpanAssert thenASingleSpanSet() {
		return SpanAssert.then(tracer.onlySpan())
				.hasRemoteServiceNameEqualTo("H2")
				.hasNameEqualTo(R2dbcObservation.R2DBC_QUERY_OBSERVATION.getContextualName())
				.hasTag("additional.tag", "bar")
				.hasTag(R2dbcObservation.LowCardinalityTags.CONNECTION.getKey(), "H2")
				.hasTag(R2dbcObservation.LowCardinalityTags.THREAD.getKey(), "test-thread")
				.hasTag(R2dbcObservation.LowCardinalityTags.URL.getKey(), "http://localhost:6543")
				.hasTag(R2dbcObservation.LowCardinalityTags.BEAN_NAME.getKey(), "hello")
				.hasTag(String.format(R2dbcObservation.HighCardinalityTags.QUERY.getKey(), "0"), "foo")
				.hasIpEqualTo("localhost")
				.hasPortThatIsSet()
				.hasKindEqualTo(Span.Kind.CLIENT);
	}

	@Override
	void thenObservabilityGotAppliedWithException(MeterRegistry registry) {
		thenASingleSpanSet()
				.thenThrowable()
				.isInstanceOf(RuntimeException.class)
				.hasMessage("Boom!");
	}

	@Override
	MeterRegistry registry() {
		MeterRegistry meterRegistry = super.registry();
		meterRegistry.observationConfig().observationHandler(new R2dbcObservationTracingHandler(this.tracer));
		return meterRegistry;
	}
}
