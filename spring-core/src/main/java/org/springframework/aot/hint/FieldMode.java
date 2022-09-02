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

package org.springframework.aot.hint;

import java.lang.reflect.Field;

import org.springframework.lang.Nullable;

/**
 * Represents the need of reflection for a given {@link Field}.
 *
 * @author Phillip Webb
 * @since 6.0
 * @see ReflectionHints
 */
public enum FieldMode {

	/**
	 * Only field read is required.
	 */
	READ,

	/**
	 * Full field read and write is required.
	 */
	WRITE;

	/**
	 * Specify if this mode already includes the specified {@code other} mode.
	 * @param other the other mode to check
	 * @return {@code true} if this mode includes the other mode
	 */
	public boolean includes(@Nullable FieldMode other) {
		return (other == null || this.ordinal() >= other.ordinal());
	}

}
