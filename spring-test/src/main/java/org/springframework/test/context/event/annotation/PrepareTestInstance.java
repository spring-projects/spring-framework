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

package org.springframework.test.context.event.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.event.PrepareTestInstanceEvent;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link EventListener @EventListener} annotation used to consume a
 * {@link PrepareTestInstanceEvent} published by the
 * {@link org.springframework.test.context.event.EventPublishingTestExecutionListener
 * EventPublishingTestExecutionListener}.
 *
 * <p>This annotation may be used on {@code @EventListener}-compliant methods within
 * a Spring test {@link org.springframework.context.ApplicationContext ApplicationContext}
 * &mdash; for example, on methods in a
 * {@link org.springframework.context.annotation.Configuration @Configuration}
 * class. A method annotated with this annotation will be invoked as part of the
 * {@link org.springframework.test.context.TestExecutionListener#prepareTestInstance}
 * lifecycle.
 *
 * <p>Event processing can optionally be made {@linkplain #value conditional} via
 * a SpEL expression &mdash; for example,
 * {@code @PrepareTestInstance("event.testContext.testClass.name matches '.+IntegrationTests'")}.
 *
 * <p>The {@code EventPublishingTestExecutionListener} must be registered in order
 * for this annotation to have an effect &mdash; for example, via
 * {@link org.springframework.test.context.TestExecutionListeners @TestExecutionListeners}.
 * Note, however, that the {@code EventPublishingTestExecutionListener} is registered
 * by default.
 *
 * @author Frank Scheffler
 * @author Sam Brannen
 * @since 5.2
 * @see PrepareTestInstanceEvent
 */
@Retention(RUNTIME)
@Target({ METHOD, ANNOTATION_TYPE })
@Documented
@EventListener(PrepareTestInstanceEvent.class)
public @interface PrepareTestInstance {

	/**
	 * Alias for {@link EventListener#condition}.
	 */
	@AliasFor(annotation = EventListener.class, attribute = "condition")
	String value() default "";

}
