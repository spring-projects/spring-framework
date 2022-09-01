/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.hint.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * Indicates that one or more {@link Class} reflection hints should be registered for
 * data binding purpose (class, fields, properties, record components for the whole
 * type hierarchy).
 *
 * <p>Typically used to annotate the bean class or bean method where the reflection hint
 * is needed.
 *
 * @author Sebastien Deleuze
 * @since 6.0
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective(RegisterReflectionForBindingProcessor.class)
public @interface RegisterReflectionForBinding {

	/**
	 * Classes for which reflection hints should be registered to enable data binding
	 * (class, fields, properties, record components for the whole type hierarchy).
	 * @see #classes()
	 */
	@AliasFor("classes")
	Class<?>[] value() default {};

	/**
	 * Classes for which reflection hints should be registered to enable data binding
	 * (class, fields, properties, record components for the whole type hierarchy).
	 * @see #value()
	 */
	@AliasFor("value")
	Class<?>[] classes() default {};

}
