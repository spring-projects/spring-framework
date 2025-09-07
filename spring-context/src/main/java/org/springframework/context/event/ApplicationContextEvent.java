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
import org.springframework.context.ApplicationEvent;

/**
 * Base class for events raised for an {@link ApplicationContext}.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
@SuppressWarnings("serial")
public abstract class ApplicationContextEvent extends ApplicationEvent {

	/**
	 * Create a new {@code ApplicationContextEvent}.
	 * @param source the {@link ApplicationContext} that the event is raised for
	 * (must not be {@code null})
	 */
	public ApplicationContextEvent(ApplicationContext source) {
		super(source);
	}

	/**
	 * Get the {@link ApplicationContext} that the event was raised for.
	 * @return the {@code ApplicationContext} that the event was raised for
	 * @since 7.0
	 * @see #getApplicationContext()
	 */
	@Override
	public ApplicationContext getSource() {
		return getApplicationContext();
	}

	/**
	 * Get the {@link ApplicationContext} that the event was raised for.
	 * @see #getSource()
	 */
	public final ApplicationContext getApplicationContext() {
		return (ApplicationContext) super.getSource();
	}

}
