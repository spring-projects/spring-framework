/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.metrics.micrometer;

import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

/**
 * {@link ApplicationStartup} implementation for micrometer.
 *
 * @author Marcin Grzejszczak
 * @since 6.0
 */
public class MicrometerApplicationStartup implements ApplicationStartup {

	private final MeterRegistry registry;

	private final AtomicReference<Timer.Sample> rootSample = new AtomicReference<>();

	public MicrometerApplicationStartup(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public StartupStep start(String name) {
		if (this.rootSample.get() == null) {
			this.rootSample.set(Timer.start(this.registry));
		}
		return new MicrometerStartupStep(name, this.registry);
	}

	public void stopRootSample() {
		this.rootSample.get().stop(Timer.builder("application-context"));
	}

}
