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
 * Indicate that the annotated element requires reflection.
 *
 * <p>When present, either directly or as a meta-annotation, this annotation
 * triggers the configured {@linkplain ReflectiveProcessor processors} against
 * the annotated element. By default, a reflection hint is registered for the
 * annotated element so that it can be discovered and invoked if necessary.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 6.0
 * @see SimpleReflectiveProcessor
 * @see ReflectiveRuntimeHintsRegistrar
 * @see RegisterReflectionForBinding @RegisterReflectionForBinding
 */
@Target({ ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.CONSTRUCTOR,
		ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Reflective {

	/**
	 * Alias for {@link #processors()}.
	 */
	@AliasFor("processors")
	Class<? extends ReflectiveProcessor>[] value() default SimpleReflectiveProcessor.class;

	/**
	 * {@link ReflectiveProcessor} implementations to invoke against the
	 * annotated element.
	 */
	@AliasFor("value")
	Class<? extends ReflectiveProcessor>[] processors() default SimpleReflectiveProcessor.class;

}
