/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.test.context.TestContext;

/**
 * Base class for events published by the {@link EventPublishingTestExecutionListener}.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 */
@SuppressWarnings("serial")
public abstract class TestContextEvent extends ApplicationEvent {

	/**
	 * Create a new {@code TestContextEvent}.
	 * @param source the {@code TestContext} associated with this event
	 * (must not be {@code null})
	 */
	public TestContextEvent(TestContext source) {
		super(source);
	}

	/**
	 * Get the {@link TestContext} associated with this event.
	 * @return the {@code TestContext} associated with this event (never {@code null})
	 * @see #getTestContext()
	 */
	@Override
	public final TestContext getSource() {
		return (TestContext) super.getSource();
	}

	/**
	 * Alias for {@link #getSource()}.
	 * <p>This method may be favored over {@code getSource()} &mdash; for example,
	 * to improve readability in SpEL expressions for event processing
	 * {@linkplain org.springframework.context.event.EventListener#condition conditions}.
	 * @return the {@code TestContext} associated with this event (never {@code null})
	 * @see #getSource()
	 */
	public final TestContext getTestContext() {
		return getSource();
	}

}
