/*
 * Copyright 2002-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.event.annotation;

import org.springframework.context.event.EventListener;
import org.springframework.test.context.event.BeforeTestExecutionEvent;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * {@link EventListener} annotation used to consume {@link BeforeTestExecutionEvent}s published
 * by {@link org.springframework.test.context.event.EventPublishingTestExecutionListener}.
 *
 * <p>This annotation may be used on {@link EventListener}-compliant methods within the Spring test
 * {@link org.springframework.context.ApplicationContext}, typically within
 * {@link org.springframework.context.annotation.Configuration}s. A method annotated hereby will be
 * called as part of the {@link org.springframework.test.context.TestExecutionListener#beforeTestExecution(org.springframework.test.context.TestContext)}
 * life-cycle method.
 *
 * <p>Make sure {@link org.springframework.test.context.event.EventPublishingTestExecutionListener} is enabled,
 * for this annotation to have an effect, e.g. by annotation your test class with
 * {@link org.springframework.test.context.TestExecutionListeners} accordingly.
 *
 * @author Frank Scheffler
 * @since 5.2
 * @see BeforeTestExecutionEvent
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD, ANNOTATION_TYPE})
@EventListener(BeforeTestExecutionEvent.class)
public @interface BeforeTestExecution {
}
