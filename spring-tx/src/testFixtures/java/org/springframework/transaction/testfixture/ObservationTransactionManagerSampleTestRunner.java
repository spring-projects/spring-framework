/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.transaction.testfixture;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.reporter.BuildingBlocks;
import io.micrometer.tracing.test.simple.SpansAssert;

/**
 *
 * @author Marcin Grzejszczak
 * @since 6.0.0
 */
public abstract class ObservationTransactionManagerSampleTestRunner<T> extends SampleTestRunner {

	public ObservationTransactionManagerSampleTestRunner() {
		super(SampleRunnerConfig.builder().build());
	}

	@Override
	public SampleTestRunnerConsumer yourCode() throws Exception {
		return (bb, meterRegistry) -> {
			T sut = given(getObservationRegistry());

			when(sut);

			Tag isolationLevelTag = Tag.of("spring.tx.isolation-level", "-1");
			Tag propagationLevelTag = Tag.of("spring.tx.propagation-level", "REQUIRED");
			Tag readOnlyTag = Tag.of("spring.tx.read-only", "false");
			Tag transactionManagerTag = Tag.of("spring.tx.transaction-manager", "org.springframework.transaction.testfixture.CallCountingTransactionManager");
			thenMeterRegistryHasATxMetric(meterRegistry, isolationLevelTag, propagationLevelTag, readOnlyTag, transactionManagerTag);
			thenThereIsATxSpan(bb, isolationLevelTag, propagationLevelTag, readOnlyTag, transactionManagerTag);
		};
	}

	protected abstract T given(ObservationRegistry observationRegistry) throws Exception;

	protected abstract void when(T sut) throws Exception;

	private void thenMeterRegistryHasATxMetric(MeterRegistry meterRegistry, Tag isolationLevelTag, Tag propagationLevelTag, Tag readOnlyTag, Tag transactionManagerTag) {
		MeterRegistryAssert.then(meterRegistry)
				.hasTimerWithNameAndTags("spring.tx",
						Tags.of(isolationLevelTag, propagationLevelTag, readOnlyTag, transactionManagerTag));
	}

	private void thenThereIsATxSpan(BuildingBlocks bb, Tag isolationLevelTag, Tag propagationLevelTag, Tag readOnlyTag, Tag transactionManagerTag) {
		SpansAssert.then(bb.getFinishedSpans())
					.thenASpanWithNameEqualTo("tx")
					.hasTag(isolationLevelTag.getKey(), isolationLevelTag.getValue())
					.hasTag(propagationLevelTag.getKey(), propagationLevelTag.getValue())
					.hasTag(readOnlyTag.getKey(), readOnlyTag.getValue())
					.hasTag(transactionManagerTag.getKey(), transactionManagerTag.getValue())
				.backToSpans()
				.hasSize(1);
	}
}
