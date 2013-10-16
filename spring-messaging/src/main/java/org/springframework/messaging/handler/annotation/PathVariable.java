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
 * Annotation which indicates that a method parameter should be bound to a path template
 * variable. Supported for {@link org.springframework.messaging.simp.annotation.SubscribeEvent},
 * {@link org.springframework.messaging.simp.annotation.UnsubscribeEvent},
 * {@link org.springframework.messaging.handler.annotation.MessageMapping}
 * annotated handler methods.
 *
 * <p>A {@code @PathVariable} template variable is always required and does not have
 * a default value to fall back on.
 *
 * @author Brian Clozel
 * @see org.springframework.messaging.simp.annotation.SubscribeEvent
 * @see org.springframework.messaging.simp.annotation.UnsubscribeEvent
 * @see org.springframework.messaging.handler.annotation.MessageMapping
 * @since 4.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PathVariable {

	/** The path template variable to bind to. */
	String value() default "";
}
