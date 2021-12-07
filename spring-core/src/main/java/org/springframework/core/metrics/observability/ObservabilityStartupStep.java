/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.core.metrics.observability;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import org.springframework.core.metrics.StartupStep;
import org.springframework.util.StringUtils;

/**
 * {@link StartupStep} implementation for Spring Observability.
 *
 * @author Marcin Grzejszczak
 */
class ObservabilityStartupStep implements StartupStep {

	private final Timer.Sample sample;

	private final MeterRegistry meterRegistry;

	private final String name;

	private final Map<String, String> tags = new HashMap<>();

	public ObservabilityStartupStep(String name, MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
		this.sample = Timer.start(meterRegistry, new ObservabilityHandlerContext(this.tags));
		this.name = name;
		this.tags.put("event", name);
	}

	private String nameFromEvent(String name) {
		String[] split = name.split("\\.");
		if (split.length > 1) {
			return split[split.length - 2] + "-" + split[split.length - 1];
		}
		return name;
	}

	private String name(String name) {
		String afterDotOrDollar = afterDotOrDollar(name);
		int index = afterDotOrDollar.lastIndexOf("@");
		if (index != -1) {
			return afterDotOrDollar.substring(0, index);
		}
		return afterDotOrDollar;
	}

	private String afterDotOrDollar(String name) {
		int index = name.lastIndexOf("$");
		if (index != -1) {
			return name.substring(index + 1);
		}
		index = name.lastIndexOf(".");
		if (index != -1) {
			return name.substring(index + 1);
		}
		return name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public long getId() {
		return 0L;
	}

	@Override
	public Long getParentId() {
		return 0L;
	}

	@Override
	public StartupStep tag(String key, String value) {
		// This comes from Boot - what do we do about this?
		if (key.equals("beanName") || key.equals("postProcessor")) {
			this.tags.put("event", EventNameUtil.toLowerHyphen(name(value)));
		}
		this.tags.put(key, value);
		return this;
	}

	@Override
	public StartupStep tag(String key, Supplier<String> value) {
		this.tags.put(key, value.get());
		return this;
	}

	@Override
	public Tags getTags() {
		return Collections::emptyIterator;
	}

	@Override
	public void end() {
		this.sample.stop(Timer.builder(nameFromEvent(this.name)));
	}

	static class ObservabilityHandlerContext extends Timer.HandlerContext {

		private final Map<String, String> tags;

		ObservabilityHandlerContext(Map<String, String> tags) {
			this.tags = tags;
		}

		@Override
		public io.micrometer.core.instrument.Tags getHighCardinalityTags() {
			return io.micrometer.core.instrument.Tags.of(this.tags.entrySet().stream()
					.map(e -> io.micrometer.core.instrument.Tag.of(e.getKey(), e.getValue()))
					.collect(Collectors.toList()));
		}
	}

	static final class EventNameUtil {

		static final int MAX_NAME_LENGTH = 50;

		private EventNameUtil() {

		}

		/**
		 * Shortens the name of a span.
		 * @param name name to shorten
		 * @return shortened name
		 */
		public static String shorten(String name) {
			if (!StringUtils.hasText(name)) {
				return name;
			}
			int maxLength = Math.min(name.length(), MAX_NAME_LENGTH);
			return name.substring(0, maxLength);
		}

		/**
		 * Converts the name to a lower hyphen version.
		 * @param name name to change
		 * @return changed name
		 */
		public static String toLowerHyphen(String name) {
			StringBuilder result = new StringBuilder();
			for (int i = 0; i < name.length(); i++) {
				char c = name.charAt(i);
				if (Character.isUpperCase(c)) {
					if (i != 0) {
						result.append('-');
					}
					result.append(Character.toLowerCase(c));
				}
				else {
					result.append(c);
				}
			}
			return EventNameUtil.shorten(result.toString());
		}

	}
}
