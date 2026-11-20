/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint.annotation;

import java.lang.reflect.AnnotatedElement;

import org.springframework.aot.hint.ReflectionHints;

/**
 * Process an {@link AnnotatedElement} and register the necessary reflection
 * hints for it.
 *
 * <p>{@code ReflectiveProcessor} implementations are registered via
 * {@link Reflective#processors() @Reflective(processors = ...)}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 * @see Reflective @Reflective
 */
public interface ReflectiveProcessor {

	/**
	 * Register {@link ReflectionHints} against the specified {@link AnnotatedElement}.
	 * @param hints the reflection hints instance to use
	 * @param element the element to process
	 */
	void registerReflectionHints(ReflectionHints hints, AnnotatedElement element);

}
