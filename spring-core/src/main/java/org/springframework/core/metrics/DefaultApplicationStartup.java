/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.metrics;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

/**
 * Default "no op" {@code ApplicationStartup} implementation.
 *
 * <p>This variant is designed for minimal overhead and does not record events.
 *
 * @author Brian Clozel
 */
class DefaultApplicationStartup implements ApplicationStartup {

	private static final DefaultStartupStep DEFAULT_STARTUP_STEP = new DefaultStartupStep();

	@Override
	public DefaultStartupStep start(String name) {
		return DEFAULT_STARTUP_STEP;
	}


	static class DefaultStartupStep implements StartupStep {

		private final DefaultTags TAGS = new DefaultTags();

		@Override
		public String getName() {
			return "default";
		}

		@Override
		public long getId() {
			return 0L;
		}

		@Override
		public @Nullable Long getParentId() {
			return null;
		}

		@Override
		public Tags getTags() {
			return this.TAGS;
		}

		@Override
		public StartupStep tag(String key, String value) {
			return this;
		}

		@Override
		public StartupStep tag(String key, Supplier<String> value) {
			return this;
		}

		@Override
		public void end() {
		}


		static class DefaultTags implements StartupStep.Tags {

			@Override
			public Iterator<StartupStep.Tag> iterator() {
				return Collections.emptyIterator();
			}
		}
	}

}
