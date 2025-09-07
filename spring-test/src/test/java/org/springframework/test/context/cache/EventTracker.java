/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.cache;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextPausedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextRestartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.test.context.event.TestContextEvent;
import org.springframework.test.context.event.annotation.AfterTestClass;
import org.springframework.test.context.event.annotation.BeforeTestClass;

/**
 * @author Sam Brannen
 * @since 7.0
 */
@Component
class EventTracker {

	static final List<String> events = new ArrayList<>();


	@EventListener(ContextRefreshedEvent.class)
	void contextRefreshed(ContextRefreshedEvent event) {
		trackApplicationContextEvent(event);
	}

	@EventListener(ContextRestartedEvent.class)
	void contextRestarted(ContextRestartedEvent event) {
		trackApplicationContextEvent(event);
	}

	@EventListener(ContextPausedEvent.class)
	void contextPaused(ContextPausedEvent event) {
		trackApplicationContextEvent(event);
	}

	@EventListener(ContextClosedEvent.class)
	void contextClosed(ContextClosedEvent event) {
		trackApplicationContextEvent(event);
	}

	@BeforeTestClass
	void beforeTestClass(TestContextEvent event) {
		trackTestContextEvent(event);
	}

	@AfterTestClass
	void afterTestClass(TestContextEvent event) {
		trackTestContextEvent(event);
	}

	private static void trackApplicationContextEvent(ApplicationContextEvent event) {
		events.add(eventName(event) + ":" + event.getSource().getDisplayName());
	}

	private static void trackTestContextEvent(TestContextEvent event) {
		events.add(eventName(event) + ":" +
				event.getSource().getTestClass().getSimpleName());
	}

	private static String eventName(ApplicationEvent event) {
		String name = event.getClass().getSimpleName();
		return name.substring(0, name.length() - "Event".length());
	}

}
