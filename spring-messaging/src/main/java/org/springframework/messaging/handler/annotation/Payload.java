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

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.messaging.converter.MessageConverter;

/**
 * Annotation that binds a method parameter to the payload of a message. Can also
 * be used to associate a payload to a method invocation. The payload may be passed
 * through a {@link MessageConverter} to convert it from serialized form with a
 * specific MIME type to an Object matching the target method parameter.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Payload {

	/**
	 * Alias for {@link #expression}.
	 */
	@AliasFor("expression")
	String value() default "";

	/**
	 * A SpEL expression to be evaluated against the payload object as the root context.
	 * <p>This attribute may or may not be supported depending on whether the message being
	 * handled contains a non-primitive Object as its payload or is in serialized form and
	 * requires message conversion.
	 * <p>When processing STOMP over WebSocket messages this attribute is not supported.
	 * @since 4.2
	 */
	@AliasFor("value")
	String expression() default "";

	/**
	 * Whether payload content is required.
	 * <p>Default is {@code true}, leading to an exception if there is no payload. Switch
	 * to {@code false} to have {@code null} passed when there is no payload.
	 */
	boolean required() default true;

}
