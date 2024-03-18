/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.metrics;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;

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
		@Nullable
		public Long getParentId() {
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
