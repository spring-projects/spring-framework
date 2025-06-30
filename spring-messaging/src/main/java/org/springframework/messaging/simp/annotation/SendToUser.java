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

package org.springframework.messaging.simp.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Indicates the return value of a message-handling method should be sent as a
 * {@link org.springframework.messaging.Message} to the specified destination(s)
 * further prepended with <code>"/user/{username}"</code> where the user name
 * is extracted from the headers of the input message being handled.
 *
 * <p>Both {@code @SendTo} and {@code @SendToUser} may be used on the same method
 * in which case a message is sent to the destinations of both annotations.
 *
 * <p>This annotation may be placed class-level in which case it is inherited
 * by methods of the class. At the same time, method-level {@code @SendTo} or
 * {@code @SendToUser} annotations override any such at the class level.

 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 * @see org.springframework.messaging.simp.annotation.support.SendToMethodReturnValueHandler
 * @see org.springframework.messaging.simp.user.UserDestinationMessageHandler
 * @see org.springframework.messaging.simp.SimpMessageHeaderAccessor#getUser()
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendToUser {

	/**
	 * Alias for {@link #destinations}.
	 * @see #destinations
	 */
	@AliasFor("destinations")
	String[] value() default {};

	/**
	 * One or more destinations to send a message to.
	 * <p>If left unspecified, a default destination is selected based on the
	 * destination of the input message being handled.
	 * @since 4.2
	 * @see #value
	 * @see org.springframework.messaging.simp.annotation.support.SendToMethodReturnValueHandler
	 */
	@AliasFor("value")
	String[] destinations() default {};

	/**
	 * Whether messages should be sent to all sessions associated with the user
	 * or only to the session of the input message being handled.
	 * <p>By default, this is set to {@code true} in which case messages are
	 * broadcast to all sessions.
	 */
	boolean broadcast() default true;

}
