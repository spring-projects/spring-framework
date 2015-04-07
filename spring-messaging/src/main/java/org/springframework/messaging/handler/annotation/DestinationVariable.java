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

package org.springframework.messaging.handler.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that indicates a method parameter should be bound to a template variable
 * in a destination template string. Supported on message handling methods such as
 * {@link MessageMapping @MessageMapping}.
 * <p>
 * A {@code @DestinationVariable} template variable is always required.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see org.springframework.messaging.handler.annotation.MessageMapping
 * @see org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DestinationVariable {

	/**
	 * The name of the destination template variable to bind to.
	 */
	String value() default "";

}
