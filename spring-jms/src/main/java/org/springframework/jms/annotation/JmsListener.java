/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.jms.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.handler.annotation.MessageMapping;

/**
 * Annotation that marks a method to be the target of a JMS message listener on the
 * specified {@link #destination}. The {@link #containerFactory} identifies the
 * {@link org.springframework.jms.config.JmsListenerContainerFactory} to use to build
 * the JMS listener container. If not set, a <em>default</em> container factory is
 * assumed to be available with a bean name of {@code jmsListenerContainerFactory}
 * unless an explicit default has been provided through configuration.
 *
 * <p><b>Consider setting up a custom
 * {@link org.springframework.jms.config.DefaultJmsListenerContainerFactory} bean.</b>
 * For production purposes, you'll typically fine-tune timeouts and recovery settings.
 * Most importantly, the default 'AUTO_ACKNOWLEDGE' mode does not provide reliability
 * guarantees, so make sure to use transacted sessions in case of reliability needs.
 *
 * <p>Processing of {@code @JmsListener} annotations is performed by registering a
 * {@link JmsListenerAnnotationBeanPostProcessor}. This can be done manually or,
 * more conveniently, through the {@code <jms:annotation-driven/>} element or
 * {@link EnableJms @EnableJms} annotation.
 *
 * <p>Annotated JMS listener methods are allowed to have flexible signatures similar
 * to what {@link MessageMapping} provides:
 * <ul>
 * <li>{@link jakarta.jms.Session} to get access to the JMS session</li>
 * <li>{@link jakarta.jms.Message} or one of its subclasses to get access to the raw JMS message</li>
 * <li>{@link org.springframework.messaging.Message} to use Spring's messaging abstraction counterpart</li>
 * <li>{@link org.springframework.messaging.handler.annotation.Payload @Payload}-annotated method
 * arguments, including support for validation</li>
 * <li>{@link org.springframework.messaging.handler.annotation.Header @Header}-annotated method
 * arguments to extract specific header values, including standard JMS headers defined by
 * {@link org.springframework.jms.support.JmsHeaders}</li>
 * <li>{@link org.springframework.messaging.handler.annotation.Headers @Headers}-annotated
 * method argument that must also be assignable to {@link java.util.Map} for obtaining
 * access to all headers</li>
 * <li>{@link org.springframework.messaging.MessageHeaders} arguments for obtaining
 * access to all headers</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor} or
 * {@link org.springframework.jms.support.JmsMessageHeaderAccessor} for convenient
 * access to all method arguments</li>
 * </ul>
 *
 * <p>Annotated methods may have a non-{@code void} return type. When they do,
 * the result of the method invocation is sent as a JMS reply to the destination
 * defined by the {@code JMSReplyTO} header of the incoming message. If this header
 * is not set, a default destination can be provided by adding
 * {@link org.springframework.messaging.handler.annotation.SendTo @SendTo} to the
 * method declaration.
 *
 * <p>This annotation can be used as a <em>{@linkplain Repeatable repeatable}</em>
 * annotation.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em> with attribute overrides.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see EnableJms
 * @see JmsListenerAnnotationBeanPostProcessor
 * @see JmsListeners
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Repeatable(JmsListeners.class)
@MessageMapping
public @interface JmsListener {

	/**
	 * The unique identifier of the container managing this endpoint.
	 * <p>If none is specified, an auto-generated one is provided.
	 * @see org.springframework.jms.config.JmsListenerEndpointRegistry#getListenerContainer(String)
	 */
	String id() default "";

	/**
	 * The bean name of the {@link org.springframework.jms.config.JmsListenerContainerFactory}
	 * to use to create the message listener container responsible for serving this endpoint.
	 * <p>If not specified, the default container factory is used, if any.
	 */
	String containerFactory() default "";

	/**
	 * The destination name for this listener, resolved through the container-wide
	 * {@link org.springframework.jms.support.destination.DestinationResolver} strategy.
	 */
	String destination();

	/**
	 * The name for the durable subscription, if any.
	 */
	String subscription() default "";

	/**
	 * The JMS message selector expression, if any.
	 * <p>See the JMS specification for a detailed definition of selector expressions.
	 */
	String selector() default "";

	/**
	 * The concurrency limits for the listener, if any. Overrides the value defined
	 * by the container factory used to create the listener container.
	 * <p>The concurrency limits can be a "lower-upper" String &mdash; for example,
	 * "5-10" &mdash; or a simple upper limit String &mdash; for example, "10", in
	 * which case the lower limit will be 1.
	 * <p>Note that the underlying container may or may not support all features.
	 * For instance, it may not be able to scale, in which case only the upper limit
	 * is used.
	 */
	String concurrency() default "";

}
