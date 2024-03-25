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
import java.util.List;

import org.springframework.expression.PropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Utility methods for use in the AST classes.
 *
 * @author Andy Clement
 * @since 3.0.2
 */
public abstract class AstUtils {

	/**
	 * Determine the set of property accessors that should be used to try to
	 * access a property on the specified target type.
	 * <p>The accessors are considered to be in an ordered list; however, in the
	 * returned list any accessors that are exact matches for the input target
	 * type (as opposed to 'general' accessors that could work for any type) are
	 * placed at the start of the list. In addition, if there are specific
	 * accessors that exactly name the class in question and accessors that name
	 * a specific class which is a supertype of the class in question, the latter
	 * are put at the end of the specific accessors set and will be tried after
	 * exactly matching accessors but before generic accessors.
	 * @param targetType the type upon which property access is being attempted
	 * @param propertyAccessors the list of property accessors to process
	 * @return a list of accessors that should be tried in order to access the property
	 */
	public static List<PropertyAccessor> getPropertyAccessorsToTry(
			@Nullable Class<?> targetType, List<PropertyAccessor> propertyAccessors) {

		List<PropertyAccessor> specificAccessors = new ArrayList<>();
		List<PropertyAccessor> generalAccessors = new ArrayList<>();
		for (PropertyAccessor accessor : propertyAccessors) {
			Class<?>[] targets = accessor.getSpecificTargetClasses();
			if (ObjectUtils.isEmpty(targets)) {
				// generic accessor that says it can be used for any type
				generalAccessors.add(accessor);
			}
			else if (targetType != null) {
				for (Class<?> clazz : targets) {
					if (clazz == targetType) {
						// add exact matches to the specificAccessors list
						specificAccessors.add(accessor);
					}
					else if (clazz.isAssignableFrom(targetType)) {
						// add supertype matches to the front of the generalAccessors list
						generalAccessors.add(0, accessor);
					}
				}
			}
		}
		List<PropertyAccessor> accessors = new ArrayList<>(specificAccessors.size() + generalAccessors.size());
		accessors.addAll(specificAccessors);
		accessors.addAll(generalAccessors);
		return accessors;
	}

}
