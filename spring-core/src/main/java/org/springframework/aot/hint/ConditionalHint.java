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

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Contract for {@link RuntimeHints runtime hints} that only apply
 * if the described condition is met.
 *
 * @author Brian Clozel
 * @since 6.0
 */
public interface ConditionalHint {

	/**
	 * Return the type that should be reachable for this hint to apply, or
	 * {@code null} if this hint should always been applied.
	 * @return the reachable type, if any
	 */
	@Nullable
	TypeReference getReachableType();

	/**
	 * Whether the condition described for this hint is met. If it is not,
	 * the hint does not apply.
	 * <p>Instead of checking for actual reachability of a type in the
	 * application, the classpath is checked for the presence of this
	 * type as a simple heuristic.
	 * @param classLoader the current classloader
	 * @return whether the condition is met and the hint applies
	 */
	default boolean conditionMatches(ClassLoader classLoader) {
		TypeReference reachableType = getReachableType();
		if (reachableType != null) {
			return ClassUtils.isPresent(reachableType.getCanonicalName(), classLoader);
		}
		return true;
	}

}
