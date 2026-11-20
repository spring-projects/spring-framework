/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
