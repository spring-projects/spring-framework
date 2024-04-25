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

package org.springframework.expression.spel.ast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.expression.TargetedAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Utility methods for use in the AST classes.
 *
 * @author Andy Clement
 * @author Sam Brannen
 * @since 3.0.2
 */
abstract class AstUtils {

	/**
	 * Determine the set of accessors that should be used to try to access an
	 * element on the specified target object.
	 * <p>Delegates to {@link #getAccessorsToTry(Class, List)} with the type of
	 * the supplied target object.
	 * @param targetObject the object upon which element access is being attempted
	 * @param accessors the list of element accessors to process
	 * @return a list of accessors that should be tried in order to access the
	 * element on the specified target type, or an empty list if no suitable
	 * accessor could be found
	 * @since 6.2
	 */
	static <T extends TargetedAccessor> List<T> getAccessorsToTry(
			@Nullable Object targetObject, List<T> accessors) {

		Class<?> targetType = (targetObject != null ? targetObject.getClass() : null);
		return getAccessorsToTry(targetType, accessors);
	}

	/**
	 * Determine the set of accessors that should be used to try to access an
	 * element on the specified target type.
	 * <p>The accessors are considered to be in an ordered list; however, in the
	 * returned list any accessors that are exact matches for the input target
	 * type (as opposed to 'generic' accessors that could work for any type) are
	 * placed at the start of the list. In addition, if there are specific
	 * accessors that exactly name the class in question and accessors that name
	 * a specific class which is a supertype of the class in question, the latter
	 * are put at the end of the specific accessors set and will be tried after
	 * exactly matching accessors but before generic accessors.
	 * @param targetType the type upon which element access is being attempted
	 * @param accessors the list of element accessors to process
	 * @return a list of accessors that should be tried in order to access the
	 * element on the specified target type, or an empty list if no suitable
	 * accessor could be found
	 * @since 6.2
	 */
	static <T extends TargetedAccessor> List<T> getAccessorsToTry(
			@Nullable Class<?> targetType, List<T> accessors) {

		if (accessors.isEmpty()) {
			return Collections.emptyList();
		}

		List<T> exactMatches = new ArrayList<>();
		List<T> inexactMatches = new ArrayList<>();
		List<T> genericMatches = new ArrayList<>();
		for (T accessor : accessors) {
			Class<?>[] targets = accessor.getSpecificTargetClasses();
			if (ObjectUtils.isEmpty(targets)) {
				// generic accessor that says it can be used for any type
				genericMatches.add(accessor);
			}
			else if (targetType != null) {
				for (Class<?> clazz : targets) {
					if (clazz == targetType) {
						exactMatches.add(accessor);
					}
					else if (clazz.isAssignableFrom(targetType)) {
						inexactMatches.add(accessor);
					}
				}
			}
		}

		int size = exactMatches.size() + inexactMatches.size() + genericMatches.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		else {
			List<T> result = new ArrayList<>(size);
			result.addAll(exactMatches);
			result.addAll(inexactMatches);
			result.addAll(genericMatches);
			return result;
		}
	}

}
