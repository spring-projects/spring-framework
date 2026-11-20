/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

import java.lang.reflect.Method;

/**
 * Interface that defines abstract access to the annotations on a specific
 * method, in a form that does not require that method's class to be loaded yet.
 *
 * <p><strong>WARNING</strong>: If an annotation cannot be loaded because one of
 * its attributes references a {@link Class} or {@link Enum}
 * {@linkplain TypeNotPresentException that is not present in the classpath},
 * that annotation will not be accessible via the {@code MethodMetadata} API.
 * To assist with diagnosing such scenarios, you can set the log level for
 * {@code "org.springframework.core.annotation.MergedAnnotation"} to {@code DEBUG},
 * {@code INFO}, or {@code WARN}.
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @see StandardMethodMetadata
 * @see AnnotationMetadata#getAnnotatedMethods
 * @see AnnotatedTypeMetadata
 */
public interface MethodMetadata extends AnnotatedTypeMetadata {

	/**
	 * Get the name of the underlying method.
	 */
	String getMethodName();

	/**
	 * Get the fully-qualified name of the class that declares the underlying method.
	 */
	String getDeclaringClassName();

	/**
	 * Get the fully-qualified name of the underlying method's declared return type.
	 * @since 4.2
	 * @see Class#getTypeName()
	 */
	String getReturnTypeName();

	/**
	 * Determine whether the underlying method is effectively abstract:
	 * i.e. marked as abstract in a class or declared as a regular,
	 * non-default method in an interface.
	 * @since 4.2
	 */
	boolean isAbstract();

	/**
	 * Determine whether the underlying method is declared as 'static'.
	 */
	boolean isStatic();

	/**
	 * Determine whether the underlying method is marked as 'final'.
	 */
	boolean isFinal();

	/**
	 * Determine whether the underlying method is overridable,
	 * i.e. not marked as static, final, or private.
	 */
	boolean isOverridable();


	/**
	 * Factory method to create a new {@link MethodMetadata} instance
	 * for the given method using standard reflection.
	 * @param method the method to introspect
	 * @return a new {@link MethodMetadata} instance
	 * @since 7.0
	 */
	static MethodMetadata introspect(Method method) {
		return StandardMethodMetadata.from(method);
	}

}
