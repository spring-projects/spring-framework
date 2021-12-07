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

import java.util.Map;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Timer;

class StartupStepHandlerContext extends Timer.HandlerContext {

	private final Map<String, String> tags;

	StartupStepHandlerContext(Map<String, String> tags) {
		this.tags = tags;
	}

	@Override
	public io.micrometer.core.instrument.Tags getHighCardinalityTags() {
		return io.micrometer.core.instrument.Tags.of(this.tags.entrySet().stream()
				.map(e -> io.micrometer.core.instrument.Tag.of(e.getKey(), e.getValue()))
				.collect(Collectors.toList()));
	}
}
