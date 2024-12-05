/*
 * Copyright 2002-2024 the original author or authors.
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
	 * A category that represents introspection on public {@linkplain Field fields}.
	 * @deprecated with no replacement since introspection is included
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 * Use {@link #INVOKE_PUBLIC_FIELDS} if getting/setting field values is required.
	 * @see Class#getFields()
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	PUBLIC_FIELDS,

	/**
	 * A category that represents introspection on {@linkplain Class#getDeclaredFields() declared
	 * fields}: all fields defined by the class but not inherited fields.
	 * @deprecated with no replacement since introspection is included
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 * Use {@link #INVOKE_DECLARED_FIELDS} if getting/setting field values is required.
	 * @see Class#getDeclaredFields()
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	DECLARED_FIELDS,

	/**
	 * A category that represents getting/setting values on public {@linkplain Field fields}.
	 * @see Field#get(Object)
	 * @see Field#set(Object, Object)
	 * @since 7.0
	 */
	INVOKE_PUBLIC_FIELDS,

	/**
	 * A category that represents getting/setting values on declared {@linkplain Field fields}.
	 * @see Field#get(Object)
	 * @see Field#set(Object, Object)
	 * @since 7.0
	 */
	INVOKE_DECLARED_FIELDS,

	/**
	 * A category that defines public {@linkplain Constructor constructors} can
	 * be introspected but not invoked.
	 * @deprecated with no replacement since introspection is included
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 * @see Class#getConstructors()
	 * @see ExecutableMode#INTROSPECT
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	INTROSPECT_PUBLIC_CONSTRUCTORS,

	/**
	 * A category that defines {@linkplain Class#getDeclaredConstructors() all
	 * constructors} can be introspected but not invoked.
	 * @deprecated with no replacement since introspection is included
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 * @see Class#getDeclaredConstructors()
	 * @see ExecutableMode#INTROSPECT
	 */
	@Deprecated(since = "7.0", forRemoval = true)
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
	 * @deprecated with no replacement since introspection is added by default
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 * @see Class#getMethods()
	 * @see ExecutableMode#INTROSPECT
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	INTROSPECT_PUBLIC_METHODS,

	/**
	 * A category that defines {@linkplain Class#getDeclaredMethods() all
	 * methods}, excluding inherited ones, can be introspected but not invoked.
	 * @deprecated with no replacement since introspection is added by default
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 * @see Class#getDeclaredMethods()
	 * @see ExecutableMode#INTROSPECT
	 */
	@Deprecated(since = "7.0", forRemoval = true)
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
	 * @deprecated with no replacement since introspection is included
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	PUBLIC_CLASSES,

	/**
	 * A category that represents all {@linkplain Class#getDeclaredClasses()
	 * inner classes}.
	 * <p>Contrary to other categories, this does not register any particular
	 * reflection for inner classes but rather makes sure they are available
	 * via a call to {@link Class#getDeclaredClasses}.
	 * @deprecated with no replacement since introspection is included
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	DECLARED_CLASSES

}
