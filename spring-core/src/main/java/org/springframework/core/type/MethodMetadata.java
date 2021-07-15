/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.core.type;

/**
 * Interface that defines abstract access to the annotations of a specific
 * method, in a form that does not require that method's class to be loaded yet.
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

}
