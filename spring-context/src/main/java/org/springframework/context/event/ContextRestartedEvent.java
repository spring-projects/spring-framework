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

package org.springframework.context.event;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Event raised when an {@code ApplicationContext} gets restarted.
 *
 * <p>Note that {@code ContextRestartedEvent} is a specialization of
 * {@link ContextStartedEvent}.
 *
 * @author Sam Brannen
 * @since 7.0
 * @see ConfigurableApplicationContext#restart()
 * @see ContextPausedEvent
 * @see ContextStartedEvent
 */
@SuppressWarnings("serial")
public class ContextRestartedEvent extends ContextStartedEvent {

	/**
	 * Create a new {@code ContextRestartedEvent}.
	 * @param source the {@code ApplicationContext} that has been restarted
	 * (must not be {@code null})
	 */
	public ContextRestartedEvent(ApplicationContext source) {
		super(source);
	}

}
