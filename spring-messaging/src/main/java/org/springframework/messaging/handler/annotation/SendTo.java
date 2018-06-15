/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.messaging.Message;

/**
 * Annotation that indicates a method's return value should be converted to
 * a {@link Message} if necessary and sent to the specified destination.
 *
 * <p>In a typical request/reply scenario, the incoming {@link Message} may
 * convey the destination to use for the reply. In that case, that destination
 * should take precedence.
 *
 * <p>The annotation may also be placed at class-level if the provider supports
 * it to indicate that all related methods should use this destination if none
 * is specified otherwise.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @since 4.0
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SendTo {

	/**
	 * The destination for a message created from the return value of a method.
	 */
	String[] value() default {};

}
