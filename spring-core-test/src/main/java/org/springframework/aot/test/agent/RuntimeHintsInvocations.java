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

package org.springframework.aot.test.agent;

import java.util.List;
import java.util.stream.Stream;

import org.assertj.core.api.AssertProvider;

import org.springframework.aot.agent.RecordedInvocation;

/**
 * A wrapper for {@link RecordedInvocation} that is the starting point for
 * {@code RuntimeHints} AssertJ assertions.
 *
 * @author Brian Clozel
 * @since 6.0
 * @see RuntimeHintsInvocationsAssert
 */
public class RuntimeHintsInvocations implements AssertProvider<RuntimeHintsInvocationsAssert> {

	private final List<RecordedInvocation> invocations;

	RuntimeHintsInvocations(List<RecordedInvocation> invocations) {
		this.invocations = invocations;
	}

	/**
	 * Use {@code assertThat(invocations)} rather than calling this method
	 * directly.
	 */
	@Override
	public RuntimeHintsInvocationsAssert assertThat() {
		return new RuntimeHintsInvocationsAssert(this);
	}

	Stream<RecordedInvocation> recordedInvocations() {
		return this.invocations.stream();
	}

}
