/*
 * Copyright 2002-present the original author or authors.
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

import java.lang.reflect.Executable;

import org.jspecify.annotations.Nullable;

/**
 * Represent the need of reflection for a given {@link Executable}.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public enum ExecutableMode {

	/**
	 * Only retrieving the {@link Executable} and its metadata is required.
	 * @deprecated with no replacement since introspection is included
	 * when {@link ReflectionHints#registerType(Class, MemberCategory...) adding a reflection hint for a type}.
	 */
	@Deprecated(since= "7.0", forRemoval = true)
	INTROSPECT,

	/**
	 * Full reflection support is required, including the ability to invoke
	 * the {@link Executable}.
	 */
	INVOKE;

	/**
	 * Specify if this mode already includes the specified {@code other} mode.
	 * @param other the other mode to check
	 * @return {@code true} if this mode includes the other mode
	 */
	boolean includes(@Nullable ExecutableMode other) {
		return (other == null || this.ordinal() >= other.ordinal());
	}

}
