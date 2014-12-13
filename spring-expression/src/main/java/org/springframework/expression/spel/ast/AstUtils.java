/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.expression.spel.ast;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.expression.PropertyAccessor;

/**
 * Utilities methods for use in the Ast classes.
 *
 * @author Andy Clement
 * @since 3.0.2
 */
public abstract class AstUtils {

	/**
	 * Determines the set of property resolvers that should be used to try and access a
	 * property on the specified target type. The resolvers are considered to be in an
	 * ordered list, however in the returned list any that are exact matches for the input
	 * target type (as opposed to 'general' resolvers that could work for any type) are
	 * placed at the start of the list. In addition, there are specific resolvers that
	 * exactly name the class in question and resolvers that name a specific class but it
	 * is a supertype of the class we have. These are put at the end of the specific resolvers
	 * set and will be tried after exactly matching accessors but before generic accessors.
	 * @param targetType the type upon which property access is being attempted
	 * @return a list of resolvers that should be tried in order to access the property
	 */
	public static List<PropertyAccessor> getPropertyAccessorsToTry(
			Class<?> targetType, List<PropertyAccessor> propertyAccessors) {

		List<PropertyAccessor> specificAccessors = new ArrayList<PropertyAccessor>();
		List<PropertyAccessor> generalAccessors = new ArrayList<PropertyAccessor>();
		for (PropertyAccessor resolver : propertyAccessors) {
			Class<?>[] targets = resolver.getSpecificTargetClasses();
			if (targets == null) {  // generic resolver that says it can be used for any type
				generalAccessors.add(resolver);
			}
			else {
				if (targetType != null) {
					int pos = 0;
					for (Class<?> clazz : targets) {
						if (clazz == targetType) {  // put exact matches on the front to be tried first?
							specificAccessors.add(pos++, resolver);
						}
						else if (clazz.isAssignableFrom(targetType)) {  // put supertype matches at the end of the
							// specificAccessor list
							generalAccessors.add(resolver);
						}
					}
				}
			}
		}
		List<PropertyAccessor> resolvers = new LinkedList<PropertyAccessor>();
		resolvers.addAll(specificAccessors);
		resolvers.addAll(generalAccessors);
		return resolvers;
	}

}
