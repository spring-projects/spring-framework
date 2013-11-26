/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

/**
 * Annotation that can be used on methods processing an input message to indicate that the
 * method's return value should be converted to a {@link Message} and sent to the
 * specified destination with the prefix <code>"/user/{username}"</code> automatically
 * prepended with the user information expected to be the input message header
 * {@link SimpMessageHeaderAccessor#USER_HEADER}. Such user destinations may need to be
 * further resolved to actual destinations.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see org.springframework.messaging.handler.annotation.SendTo
 * @see org.springframework.messaging.simp.handler.UserDestinationMessageHandler
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendToUser {

	/**
	 * The destination for a message based on the return value of a method.
	 */
	String[] value() default {};

}
