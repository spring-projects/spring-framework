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

import jdk.jfr.Category;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;

/**
 * {@link Event} extension for recording {@link FlightRecorderStartupStep}
 * in Java Flight Recorder.
 *
 * <p>{@link org.springframework.core.metrics.StartupStep.Tags} are serialized
 * as a single {@code String}, since Flight Recorder events do not support complex types.
 *
 * @author Brian Clozel
 * @since 5.3
 */
@Category("Spring Application")
@Label("Startup Step")
@Description("Spring Application Startup")
class FlightRecorderStartupEvent extends Event {

	public final long eventId;

	public final long parentId;

	@Label("Name")
	public final String name;

	@Label("Tags")
	String tags = "";

	public FlightRecorderStartupEvent(long eventId, String name, long parentId) {
		this.name = name;
		this.eventId = eventId;
		this.parentId = parentId;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

}
