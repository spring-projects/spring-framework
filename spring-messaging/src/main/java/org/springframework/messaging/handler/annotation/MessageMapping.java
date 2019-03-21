/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.Message;

/**
 * Annotation for mapping a {@link Message} onto message-handling methods by matching
 * to the message destination. This annotation can also be used on the type-level in
 * which case it defines a common destination prefix or pattern for all method-level
 * annotations including method-level
 * {@link org.springframework.messaging.simp.annotation.SubscribeMapping @SubscribeMapping}
 * annotations.
 *
 * <p>Handler methods which are annotated with this annotation are allowed to have
 * flexible signatures. They may have arguments of the following types, in arbitrary
 * order:
 * <ul>
 * <li>{@link Message} to get access to the complete message being processed.</li>
 * <li>{@link Payload}-annotated method arguments to extract the payload of
 * a message and optionally convert it using a
 * {@link org.springframework.messaging.converter.MessageConverter}.
 * The presence of the annotation is not required since it is assumed by default
 * for method arguments that are not annotated. Payload method arguments annotated
 * with Validation annotations (like
 * {@link org.springframework.validation.annotation.Validated}) will be subject to
 * JSR-303 validation.</li>
 * <li>{@link Header}-annotated method arguments to extract a specific
 * header value along with type conversion with a
 * {@link org.springframework.core.convert.converter.Converter} if necessary.</li>
 * <li>{@link Headers}-annotated argument that must also be assignable to
 * {@link java.util.Map} for getting access to all headers.</li>
 * <li>{@link org.springframework.messaging.MessageHeaders} arguments for
 * getting access to all headers.</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor} or
 * with STOMP over WebSocket support also sub-classes such as
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor}
 * for convenient access to all method arguments.</li>
 * <li>{@link DestinationVariable}-annotated arguments for access to template
 * variable values extracted from the message destination (e.g. /hotels/{hotel}).
 * Variable values will be converted to the declared method argument type.</li>
 * <li>{@link java.security.Principal} method arguments are supported with
 * STOMP over WebSocket messages. It reflects the user logged in to the
 * WebSocket session on which the message was received. Regular HTTP-based
 * authentication (e.g. Spring Security based) can be used to secure the
 * HTTP handshake that initiates WebSocket sessions.</li>
 * </ul>
 *
 * <p>A return value will get wrapped as a message and sent to a default response
 * destination or to a custom destination specified with an {@link SendTo @SendTo}
 * method-level annotation. Such a response may also be provided asynchronously
 * via a {@link org.springframework.util.concurrent.ListenableFuture} return type
 * or a corresponding JDK 8 {@link java.util.concurrent.CompletableFuture} /
 * {@link java.util.concurrent.CompletionStage} handle.
 *
 * <h3>STOMP over WebSocket</h3>
 * <p>An {@link SendTo @SendTo} annotation is not strictly required &mdash; by default
 * the message will be sent to the same destination as the incoming message but with
 * an additional prefix ({@code "/topic"} by default). It is also possible to use the
 * {@link org.springframework.messaging.simp.annotation.SendToUser} annotation to
 * have the message directed to a specific user if connected. The return value is
 * converted with a {@link org.springframework.messaging.converter.MessageConverter}.
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @MessageMapping} and {@code @SubscribeMapping} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MessageMapping {

	/**
	 * Destination-based mapping expressed by this annotation.
	 * <p>For STOMP over WebSocket messages: this is the destination of the
	 * STOMP message (e.g. {@code "/positions"}). Ant-style path patterns
	 * (e.g. {@code "/price.stock.*"}) and path template variables (e.g.
	 * <code>"/price.stock.{ticker}"</code>) are also supported.
	 */
	String[] value() default {};

}
