/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
