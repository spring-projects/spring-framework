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

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.messaging.Message;

/**
 * Annotation for mapping a {@link Message} onto a message-handling method by
 * matching the declared {@link #value() patterns} to a destination extracted
 * from the message. The annotation is supported at the type-level too, as a
 * way of declaring a pattern prefix (or prefixes) across all class methods.
 *
 * <p>{@code @MessageMapping} methods support the following arguments:
 * <ul>
 * <li>{@link Payload @Payload} method argument to extract the payload of a
 * message and have it de-serialized to the declared target type.
 * {@code @Payload} arguments may also be annotated with Validation annotations
 * such as {@link org.springframework.validation.annotation.Validated @Validated}
 * and will then have JSR-303 validation applied. Keep in mind the annotation
 * is not required to be present as it is assumed by default for arguments not
 * handled otherwise. </li>
 * <li>{@link DestinationVariable @DestinationVariable} method argument for
 * access to template variable values extracted from the message destination,
 * e.g. {@code /hotels/{hotel}}. Variable values may also be converted from
 * String to the declared method argument type, if needed.</li>
 * <li>{@link Header @Header} method argument to extract a specific message
 * header value and have a
 * {@link org.springframework.core.convert.converter.Converter Converter}
 * applied to it to convert the value to the declared target type.</li>
 * <li>{@link Headers @Headers} method argument that is also assignable to
 * {@link java.util.Map} for access to all headers.</li>
 * <li>{@link org.springframework.messaging.MessageHeaders MessageHeaders}
 * method argument for access to all headers.</li>
 * <li>{@link org.springframework.messaging.support.MessageHeaderAccessor
 * MessageHeaderAccessor} method argument for access to all headers.
 * In some processing scenarios, like STOMP over WebSocket, this may also be
 * a specialization such as
 * {@link org.springframework.messaging.simp.SimpMessageHeaderAccessor
 * SimpMessageHeaderAccessor}.</li>
 * <li>{@link Message Message&lt;T&gt;} for access to body and headers with the body
 * de-serialized if necessary to match the declared type.</li>
 * <li>{@link java.security.Principal} method arguments are supported in
 * some processing scenarios such as STOMP over WebSocket. It reflects the
 * authenticated user.</li>
 * </ul>
 *
 * <p>Return value handling depends on the processing scenario:
 * <ul>
 * <li>STOMP over WebSocket -- the value is turned into a message and sent to a
 * default response destination or to a custom destination specified with an
 * {@link SendTo @SendTo} or
 * {@link org.springframework.messaging.simp.annotation.SendToUser @SendToUser}
 * annotation.
 * <li>RSocket -- the response is used to reply to the stream request.
 * </ul>
 *
 * <p>Specializations of this annotation include
 * {@link org.springframework.messaging.simp.annotation.SubscribeMapping @SubscribeMapping}
 * (e.g. STOMP subscriptions) and
 * {@link org.springframework.messaging.rsocket.annotation.ConnectMapping @ConnectMapping}
 * (e.g. RSocket connections). Both narrow the primary mapping further and also match
 * against the message type. Both can be combined with a type-level
 * {@code @MessageMapping} that declares a common pattern prefix (or prefixes).
 *
 * <p>For further details on the use of this annotation in different contexts,
 * see the following sections of the Spring Framework reference:
 * <ul>
 * <li>STOMP over WebSocket
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#websocket-stomp-handle-annotations">
 * "Annotated Controllers"</a>.
 * <li>RSocket
 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html#rsocket-annot-responders">
 * "Annotated Responders"</a>.
 * </ul>
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @MessageMapping} and {@code @SubscribeMapping} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
 * @see org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(MessageMappingReflectiveProcessor.class)
public @interface MessageMapping {

	/**
	 * Destination-based mapping expressed by this annotation.
	 * <p>For STOMP over WebSocket messages this is
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher}-style
	 * patterns matched against the STOMP destination of the message.
	 * <p>for RSocket this is either
	 * {@link org.springframework.util.AntPathMatcher AntPathMatcher} or
	 * {@link org.springframework.web.util.pattern.PathPattern PathPattern}
	 * based pattern, depending on which is configured, matched to the route of
	 * the stream request.
	 * <p>If no patterns are configured, the mapping matches all destinations.
	 */
	String[] value() default {};

}
