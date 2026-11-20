/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 * @see ReflectiveRuntimeHintsRegistrar
 * @see RegisterReflection @RegisterReflection
 * @see RegisterReflectionForBinding @RegisterReflectionForBinding
 */
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD})
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
