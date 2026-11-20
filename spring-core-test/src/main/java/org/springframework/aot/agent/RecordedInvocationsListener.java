/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.agent;

/**
 * Listener for {@link RecordedInvocation invocations recorded} by the {@link RuntimeHintsAgent}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
@FunctionalInterface
public interface RecordedInvocationsListener {

	/**
	 * Called when an {@link RecordedInvocation invocation has been recorded} by the Java Agent.
	 * @param invocation the recorded invocation.
	 */
	void onInvocation(RecordedInvocation invocation);

}
