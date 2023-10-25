/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.context.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.PayloadApplicationEvent;

/**
 * Default implementation of {@link ApplicationEvents}.
 *
 * @author Oliver Drotbohm
 * @author Sam Brannen
 * @since 5.3.3
 */
class DefaultApplicationEvents implements ApplicationEvents {

	private final List<ApplicationEvent> events = new CopyOnWriteArrayList<>();


	void addEvent(ApplicationEvent event) {
		this.events.add(event);
	}

	@Override
	public Stream<ApplicationEvent> stream() {
		return this.events.stream();
	}

	@Override
	public <T> Stream<T> stream(Class<T> type) {
		return this.events.stream()
				.map(this::unwrapPayloadEvent)
				.filter(type::isInstance)
				.map(type::cast);
	}

	@Override
	public void clear() {
		this.events.clear();
	}

	private Object unwrapPayloadEvent(Object source) {
		return ((source instanceof PayloadApplicationEvent<?> payloadApplicationEvent) ?
				payloadApplicationEvent.getPayload() : source);
	}

}
