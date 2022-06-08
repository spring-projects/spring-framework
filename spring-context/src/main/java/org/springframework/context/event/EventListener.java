/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation that marks a method as a listener for application events.
 *
 * <p>If an annotated method supports a single event type, the method may
 * declare a single parameter that reflects the event type to listen to.
 * If an annotated method supports multiple event types, this annotation
 * may refer to one or more supported event types using the {@code classes}
 * attribute. See the {@link #classes} javadoc for further details.
 *
 * <p>Events can be {@link ApplicationEvent} instances as well as arbitrary
 * objects.
 *
 * <p>Processing of {@code @EventListener} annotations is performed via
 * the internal {@link EventListenerMethodProcessor} bean which gets
 * registered automatically when using Java config or manually via the
 * {@code <context:annotation-config/>} or {@code <context:component-scan/>}
 * element when using XML config.
 *
 * <p>Annotated methods may have a non-{@code void} return type. When they
 * do, the result of the method invocation is sent as a new event. If the
 * return type is either an array or a collection, each element is sent
 * as a new individual event.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <h3>Exception Handling</h3>
 * <p>While it is possible for an event listener to declare that it
 * throws arbitrary exception types, any checked exceptions thrown
 * from an event listener will be wrapped in an
 * {@link java.lang.reflect.UndeclaredThrowableException UndeclaredThrowableException}
 * since the event publisher can only handle runtime exceptions.
 *
 * <h3>Asynchronous Listeners</h3>
 * <p>If you want a particular listener to process events asynchronously, you
 * can use Spring's {@link org.springframework.scheduling.annotation.Async @Async}
 * support, but be aware of the following limitations when using asynchronous events.
 *
 * <ul>
 * <li>If an asynchronous event listener throws an exception, it is not propagated
 * to the caller. See {@link org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler
 * AsyncUncaughtExceptionHandler} for more details.</li>
 * <li>Asynchronous event listener methods cannot publish a subsequent event by returning a
 * value. If you need to publish another event as the result of the processing, inject an
 * {@link org.springframework.context.ApplicationEventPublisher ApplicationEventPublisher}
 * to publish the event manually.</li>
 * </ul>
 *
 * <h3>Ordering Listeners</h3>
 * <p>It is also possible to define the order in which listeners for a
 * certain event are to be invoked. To do so, add Spring's common
 * {@link org.springframework.core.annotation.Order @Order} annotation
 * alongside this event listener annotation.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.2
 * @see EventListenerMethodProcessor
 * @see org.springframework.transaction.event.TransactionalEventListener
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface EventListener {

	/**
	 * Alias for {@link #classes}.
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * The event classes that this listener handles.
	 * <p>If this attribute is specified with a single value, the
	 * annotated method may optionally accept a single parameter.
	 * However, if this attribute is specified with multiple values,
	 * the annotated method must <em>not</em> declare any parameters.
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

	/**
	 * Spring Expression Language (SpEL) expression used for making the event
	 * handling conditional.
	 * <p>The event will be handled if the expression evaluates to boolean
	 * {@code true} or one of the following strings: {@code "true"}, {@code "on"},
	 * {@code "yes"}, or {@code "1"}.
	 * <p>The default expression is {@code ""}, meaning the event is always handled.
	 * <p>The SpEL expression will be evaluated against a dedicated context that
	 * provides the following metadata:
	 * <ul>
	 * <li>{@code #root.event} or {@code event} for references to the
	 * {@link ApplicationEvent}</li>
	 * <li>{@code #root.args} or {@code args} for references to the method
	 * arguments array</li>
	 * <li>Method arguments can be accessed by index. For example, the first
	 * argument can be accessed via {@code #root.args[0]}, {@code args[0]},
	 * {@code #a0}, or {@code #p0}.</li>
	 * <li>Method arguments can be accessed by name (with a preceding hash tag)
	 * if parameter names are available in the compiled byte code.</li>
	 * </ul>
	 */
	String condition() default "";

	/**
	 * An optional identifier for the listener, defaulting to the fully-qualified
	 * signature of the declaring method (e.g. "mypackage.MyClass.myMethod()").
	 * @since 5.3.5
	 * @see SmartApplicationListener#getListenerId()
	 * @see ApplicationEventMulticaster#removeApplicationListeners(Predicate)
	 */
	String id() default "";

}
