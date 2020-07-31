/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.core.metrics.jfr;

import java.util.ArrayDeque;
import java.util.Deque;

import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

/**
 * {@link ApplicationStartup} implementation for the Java Flight Recorder.
 * <p>This variant records {@link StartupStep} as Flight Recorder events; because such events
 * only support base types, the {@link StartupStep.Tags} are serialized as a single String attribute.
 * <p>Once this is configured on the application context, you can record data by launching the application
 * with recording enabled: {@code java -XX:StartFlightRecording:filename=recording.jfr,duration=10s -jar app.jar}.
 *
 * @author Brian Clozel
 * @since 5.3
 */
public class FlightRecorderApplicationStartup implements ApplicationStartup {

	private long currentSequenceId;

	private final Deque<Long> currentSteps;


	public FlightRecorderApplicationStartup() {
		this.currentSequenceId = 0;
		this.currentSteps = new ArrayDeque<>();
		this.currentSteps.offerFirst(0L);
	}


	@Override
	public StartupStep start(String name) {
		FlightRecorderStartupStep step = new FlightRecorderStartupStep(++this.currentSequenceId, name,
				this.currentSteps.getFirst(), committedStep -> this.currentSteps.removeFirst());
		this.currentSteps.offerFirst(this.currentSequenceId);
		return step;
	}

}
