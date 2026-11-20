/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.hint;

import org.jspecify.annotations.Nullable;

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
	@Nullable TypeReference getReachableType();

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
