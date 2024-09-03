/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.aot.hint;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Predefined {@link Member} categories.
 *
 * @author Andy Clement
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 * @since 6.0
 */
public enum MemberCategory {

	/**
	 * A category that represents public {@linkplain Field fields}.
	 * @see Class#getFields()
	 */
	PUBLIC_FIELDS,

	/**
	 * A category that represents {@linkplain Class#getDeclaredFields() declared
	 * fields}: all fields defined by the class but not inherited fields.
	 * @see Class#getDeclaredFields()
	 */
	DECLARED_FIELDS,

	/**
	 * A category that defines public {@linkplain Constructor constructors} can
	 * be introspected but not invoked.
	 * @see Class#getConstructors()
	 * @see ExecutableMode#INTROSPECT
	 */
	INTROSPECT_PUBLIC_CONSTRUCTORS,

	/**
	 * A category that defines {@linkplain Class#getDeclaredConstructors() all
	 * constructors} can be introspected but not invoked.
	 * @see Class#getDeclaredConstructors()
	 * @see ExecutableMode#INTROSPECT
	 */
	INTROSPECT_DECLARED_CONSTRUCTORS,

	/**
	 * A category that defines public {@linkplain Constructor constructors} can
	 * be invoked.
	 * @see Class#getConstructors()
	 * @see ExecutableMode#INVOKE
	 */
	INVOKE_PUBLIC_CONSTRUCTORS,

	/**
	 * A category that defines {@linkplain Class#getDeclaredConstructors() all
	 * constructors} can be invoked.
	 * @see Class#getDeclaredConstructors()
	 * @see ExecutableMode#INVOKE
	 */
	INVOKE_DECLARED_CONSTRUCTORS,

	/**
	 * A category that defines public {@linkplain Method methods}, including
	 * inherited ones, can be introspected but not invoked.
	 * @see Class#getMethods()
	 * @see ExecutableMode#INTROSPECT
	 */
	INTROSPECT_PUBLIC_METHODS,

	/**
	 * A category that defines {@linkplain Class#getDeclaredMethods() all
	 * methods}, excluding inherited ones, can be introspected but not invoked.
	 * @see Class#getDeclaredMethods()
	 * @see ExecutableMode#INTROSPECT
	 */
	INTROSPECT_DECLARED_METHODS,

	/**
	 * A category that defines public {@linkplain Method methods}, including
	 * inherited ones, can be invoked.
	 * @see Class#getMethods()
	 * @see ExecutableMode#INVOKE
	 */
	INVOKE_PUBLIC_METHODS,

	/**
	 * A category that defines {@linkplain Class#getDeclaredMethods() all
	 * methods}, excluding inherited ones, can be invoked.
	 * @see Class#getDeclaredMethods()
	 * @see ExecutableMode#INVOKE
	 */
	INVOKE_DECLARED_METHODS,

	/**
	 * A category that represents public {@linkplain Class#getClasses() inner
	 * classes}.
	 * <p>Contrary to other categories, this does not register any particular
	 * reflection for inner classes but rather makes sure they are available
	 * via a call to {@link Class#getClasses}.
	 */
	PUBLIC_CLASSES,

	/**
	 * A category that represents all {@linkplain Class#getDeclaredClasses()
	 * inner classes}.
	 * <p>Contrary to other categories, this does not register any particular
	 * reflection for inner classes but rather makes sure they are available
	 * via a call to {@link Class#getDeclaredClasses}.
	 */
	DECLARED_CLASSES;

}
