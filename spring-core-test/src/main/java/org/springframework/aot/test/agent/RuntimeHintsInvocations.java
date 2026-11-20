/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
