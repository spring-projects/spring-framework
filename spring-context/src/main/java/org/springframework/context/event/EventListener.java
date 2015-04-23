/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.context.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.ApplicationEvent;

/**
 * Annotation that marks a method to listen for application events. The
 * method should have one and only one parameter that reflects the event
 * type to listen to. Events can be {@link ApplicationEvent} instances
 * as well as arbitrary objects.
 *
 * <p>Processing of {@code @EventListener} annotations is performed via
 * {@link EventListenerMethodProcessor} that is registered automatically
 * when using Java config or via the {@code <context:annotation-driven/>}
 * XML element.
 *
 * <p>Annotated methods may have a non-{@code void} return type. When they
 * do, the result of the method invocation is sent as a new event. It is
 * also possible to defined the order in which listeners for a certain
 * event are invoked. To do so, add a regular {code @Order} annotation
 * alongside this annotation.
 *
 * <p>While it is possible to define any arbitrary exception types, checked
 * exceptions will be wrapped in a {@link java.lang.reflect.UndeclaredThrowableException}
 * as the caller only handles runtime exceptions.
 *
 * @author Stephane Nicoll
 * @since 4.2
 * @see EventListenerMethodProcessor
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EventListener {

	/**
	 * Spring Expression Language (SpEL) attribute used for making the event
	 * handling conditional.
	 * <p>Default is "", meaning the event is always handled.
	 */
	String condition() default "";

}