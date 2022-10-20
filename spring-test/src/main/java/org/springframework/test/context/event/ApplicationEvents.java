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

import java.util.stream.Stream;

import org.springframework.context.ApplicationEvent;

/**
 * {@code ApplicationEvents} encapsulates all {@linkplain ApplicationEvent
 * application events} that were fired during the execution of a single test method.
 *
 * <p>To use {@code ApplicationEvents} in your tests, do the following.
 * <ul>
 * <li>Ensure that your test class is annotated or meta-annotated with
 * {@link RecordApplicationEvents @RecordApplicationEvents}.</li>
 * <li>Ensure that the {@link ApplicationEventsTestExecutionListener} is
 * registered. Note, however, that it is registered by default and only needs
 * to be manually registered if you have custom configuration via
 * {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners}
 * that does not include the default listeners.</li>
 * <li>Annotate a field of type {@code ApplicationEvents} with
 * {@link org.springframework.beans.factory.annotation.Autowired @Autowired} and
 * use that instance of {@code ApplicationEvents} in your test and lifecycle methods.</li>
 * <li>With JUnit Jupiter, you may optionally declare a parameter of type
 * {@code ApplicationEvents} in a test or lifecycle method as an alternative to
 * an {@code @Autowired} field in the test class.</li>
 * </ul>
 *
 * @author Sam Brannen
 * @author Oliver Drotbohm
 * @since 5.3.3
 * @see RecordApplicationEvents
 * @see ApplicationEventsTestExecutionListener
 * @see org.springframework.context.ApplicationEvent
 */
public interface ApplicationEvents {

	/**
	 * Stream all application events that were fired during test execution.
	 * @return a stream of all application events
	 * @see #stream(Class)
	 * @see #clear()
	 */
	Stream<ApplicationEvent> stream();

	/**
	 * Stream all application events or event payloads of the given type that
	 * were fired during test execution.
	 * @param <T> the event type
	 * @param type the type of events or payloads to stream; never {@code null}
	 * @return a stream of all application events or event payloads of the
	 * specified type
	 * @see #stream()
	 * @see #clear()
	 */
	<T> Stream<T> stream(Class<T> type);

	/**
	 * Clear all application events recorded by this {@code ApplicationEvents} instance.
	 * <p>Subsequent calls to {@link #stream()} or {@link #stream(Class)} will
	 * only include events recorded since this method was invoked.
	 * @see #stream()
	 * @see #stream(Class)
	 */
	void clear();

}
