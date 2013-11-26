/*
 * Copyright 2002-2013 the original author or authors.
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
 * <p>This is a method-level annotations that can be combined with a type-level
 * {@link org.springframework.messaging.handler.annotation.MessageMapping @MessageMapping}
 *
 * <p>Supports the same method arguments as
 * {@link org.springframework.messaging.handler.annotation.MessageMapping}, however
 * subscription messages typically do not have a body.
 *
 * <p>The return value also follows the same rules as for
 * {@link org.springframework.messaging.handler.annotation.MessageMapping} except if
 * the method is not annotated with
 * {@link org.springframework.messaging.handler.annotation.SendTo} or {@link SendToUser},
 * the message is sent directly back to the connected user and does not pass through
 * the message broker. This is useful for implementing a request-reply pattern.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SubscribeMapping {

	/**
	 * Destination-based mapping expressed by this annotation.
	 * <p>For STOMP over WebSocket messages: this is the destination of the STOMP message
	 * (e.g. "/positions"). Ant-style path patterns (e.g. "/price.stock.*") are supported
	 * and so are path template variables (e.g. "/price.stock.{ticker}"").
	 */
	String[] value() default {};

}
