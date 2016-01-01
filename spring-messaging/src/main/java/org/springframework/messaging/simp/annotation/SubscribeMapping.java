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

package org.springframework.messaging.simp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for mapping subscription messages onto specific handler methods based
 * on the destination of a subscription. Supported with STOMP over WebSocket only
 * (e.g. STOMP SUBSCRIBE frame).
 *
 * <p>This is a method-level annotation that can be combined with a type-level
 * {@link org.springframework.messaging.handler.annotation.MessageMapping @MessageMapping}.
 *
 * <p>Supports the same method arguments as {@code @MessageMapping}; however,
 * subscription messages typically do not have a body.
 *
 * <p>The return value also follows the same rules as for {@code @MessageMapping},
 * except if the method is not annotated with
 * {@link org.springframework.messaging.handler.annotation.SendTo SendTo} or
 * {@link SendToUser}, the message is sent directly back to the connected
 * user and does not pass through the message broker. This is useful for
 * implementing a request-reply pattern.
 *
 * <p><b>NOTE:</b> When using controller interfaces (e.g. for AOP proxying),
 * make sure to consistently put <i>all</i> your mapping annotations - such as
 * {@code @MessageMapping} and {@code @SubscribeMapping} - on
 * the controller <i>interface</i> rather than on the implementation class.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.handler.annotation.MessageMapping
 * @see org.springframework.messaging.handler.annotation.SendTo
 * @see org.springframework.messaging.simp.annotation.SendToUser
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscribeMapping {

	/**
	 * Destination-based mapping expressed by this annotation.
	 * <p>This is the destination of the STOMP message (e.g. {@code "/positions"}).
	 * Ant-style path patterns (e.g. {@code "/price.stock.*"}) and path template
	 * variables (e.g. <code>"/price.stock.{ticker}"</code>) are also supported.
	 */
	String[] value() default {};

}
