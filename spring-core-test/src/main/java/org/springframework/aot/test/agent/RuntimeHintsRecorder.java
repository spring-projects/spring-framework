/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.aot.agent.RecordedInvocation;
import org.springframework.aot.agent.RecordedInvocationsListener;
import org.springframework.aot.agent.RecordedInvocationsPublisher;
import org.springframework.aot.agent.RuntimeHintsAgent;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.util.Assert;

/**
 * Invocations relevant to {@link RuntimeHints} recorded during the execution of a block
 * of code instrumented by the {@link RuntimeHintsAgent}.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public final class RuntimeHintsRecorder {

	private final RuntimeHintsInvocationsListener listener;

	private RuntimeHintsRecorder() {
		this.listener = new RuntimeHintsInvocationsListener();
	}

	/**
	 * Record all method invocations relevant to {@link RuntimeHints} that happened
	 * during the execution of the given action.
	 * @param action the block of code we want to record invocations from
	 * @return the recorded invocations
	 */
	public synchronized static RuntimeHintsInvocations record(Runnable action) {
		Assert.notNull(action, "Runnable action must not be null");
		Assert.state(RuntimeHintsAgent.isLoaded(), "RuntimeHintsAgent must be loaded in the current JVM");
		RuntimeHintsRecorder recorder = new RuntimeHintsRecorder();
		RecordedInvocationsPublisher.addListener(recorder.listener);
		try {
			action.run();
		}
		finally {
			RecordedInvocationsPublisher.removeListener(recorder.listener);
		}
		return new RuntimeHintsInvocations(recorder.listener.recordedInvocations.stream().toList());
	}


	private static final class RuntimeHintsInvocationsListener implements RecordedInvocationsListener {

		private final Deque<RecordedInvocation> recordedInvocations = new ArrayDeque<>();

		@Override
		public void onInvocation(RecordedInvocation invocation) {
			this.recordedInvocations.addLast(invocation);
		}

	}

}
