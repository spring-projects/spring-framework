/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.agent;

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;

/**
 * Publishes invocations on method relevant to {@link RuntimeHints},
 * as they are recorded by the {@link RuntimeHintsAgent}.
 * <p>Components interested in this can {@link #addListener(RecordedInvocationsListener) register}
 * and {@link #removeListener(RecordedInvocationsListener) deregister} themselves at any point at runtime.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public abstract class RecordedInvocationsPublisher {

	private static final Deque<RecordedInvocationsListener> LISTENERS = new ArrayDeque<>();

	private RecordedInvocationsPublisher() {

	}

	/**
	 * Register the given invocations listener.
	 * @param listener the listener to be notified about recorded invocations
	 */
	public static void addListener(RecordedInvocationsListener listener) {
		LISTENERS.addLast(listener);
	}

	/**
	 * Deregister the given invocations listener.
	 * @param listener the listener that was notified about recorded invocations
	 */
	public static void removeListener(RecordedInvocationsListener listener) {
		LISTENERS.remove(listener);
	}

	/**
	 * Record an invocation on reflection methods covered by {@link ReflectionHints}.
	 */
	static void publish(RecordedInvocation invocation) {
		LISTENERS.forEach(listener -> listener.onInvocation(invocation));
	}

}
