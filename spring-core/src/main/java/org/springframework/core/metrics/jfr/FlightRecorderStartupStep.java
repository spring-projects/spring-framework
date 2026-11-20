/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.metrics.jfr;

import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.springframework.core.metrics.StartupStep;

/**
 * {@link StartupStep} implementation for the Java Flight Recorder.
 *
 * <p>This variant delegates to a {@link FlightRecorderStartupEvent JFR event extension}
 * to collect and record data in Java Flight Recorder.
 *
 * @author Brian Clozel
 */
class FlightRecorderStartupStep implements StartupStep {

	private final FlightRecorderStartupEvent event;

	private final FlightRecorderTags tags = new FlightRecorderTags();

	private final Consumer<FlightRecorderStartupStep> recordingCallback;


	public FlightRecorderStartupStep(long id, String name, long parentId,
			Consumer<FlightRecorderStartupStep> recordingCallback) {

		this.event = new FlightRecorderStartupEvent(id, name, parentId);
		this.event.begin();
		this.recordingCallback = recordingCallback;
	}


	@Override
	public String getName() {
		return this.event.name;
	}

	@Override
	public long getId() {
		return this.event.eventId;
	}

	@Override
	public Long getParentId() {
		return this.event.parentId;
	}

	@Override
	public StartupStep tag(String key, String value) {
		this.tags.add(key, value);
		return this;
	}

	@Override
	public StartupStep tag(String key, Supplier<String> value) {
		this.tags.add(key, value.get());
		return this;
	}

	@Override
	public Tags getTags() {
		return this.tags;
	}

	@Override
	public void end() {
		this.event.end();
		if (this.event.shouldCommit()) {
			StringBuilder builder = new StringBuilder();
			this.tags.forEach(tag ->
					builder.append(tag.getKey()).append('=').append(tag.getValue()).append(',')
			);
			this.event.setTags(builder.toString());
		}
		this.event.commit();
		this.recordingCallback.accept(this);
	}

	protected FlightRecorderStartupEvent getEvent() {
		return this.event;
	}


	static class FlightRecorderTags implements Tags {

		private Tag[] tags = new Tag[0];

		public void add(String key, String value) {
			Tag[] newTags = new Tag[this.tags.length + 1];
			System.arraycopy(this.tags, 0, newTags, 0, this.tags.length);
			newTags[newTags.length - 1] = new FlightRecorderTag(key, value);
			this.tags = newTags;
		}

		public void add(String key, Supplier<String> value) {
			add(key, value.get());
		}

		@Override
		public Iterator<Tag> iterator() {
			return new TagsIterator();
		}


		private class TagsIterator implements Iterator<Tag> {

			private int idx = 0;

			@Override
			public boolean hasNext() {
				return this.idx < tags.length;
			}

			@Override
			public Tag next() {
				return tags[this.idx++];
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException("tags are append only");
			}
		}
	}


	static class FlightRecorderTag implements Tag {

		private final String key;

		private final String value;

		public FlightRecorderTag(String key, String value) {
			this.key = key;
			this.value = value;
		}

		@Override
		public String getKey() {
			return this.key;
		}

		@Override
		public String getValue() {
			return this.value;
		}
	}

}
