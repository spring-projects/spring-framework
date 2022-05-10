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

package org.springframework.test.context.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * {@link ApplicationListener} that listens to all events and adds them to the
 * current {@link ApplicationEvents} instance if registered for the current thread.
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 */
class ApplicationEventsApplicationListener implements ApplicationListener<ApplicationEvent> {

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		DefaultApplicationEvents applicationEvents =
				(DefaultApplicationEvents) ApplicationEventsHolder.getApplicationEvents();
		if (applicationEvents != null) {
			applicationEvents.addEvent(event);
		}
	}

}
