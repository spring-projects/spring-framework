/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.core.type.filter;

/**
 * A simple filter which matches classes that are assignable to a given type.
 *
 * @author Rod Johnson
 * @author Mark Fisher
 * @author Ramnivas Laddad
 * @since 2.5
 */
public class AssignableTypeFilter extends AbstractTypeHierarchyTraversingFilter {

	private final Class<?> targetType;


	/**
	 * Create a new AssignableTypeFilter for the given type.
	 * @param targetType the type to match
	 */
	public AssignableTypeFilter(Class<?> targetType) {
		super(true, true);
		this.targetType = targetType;
	}


	@Override
	protected boolean matchClassName(String className) {
		return this.targetType.getName().equals(className);
	}

	@Override
	protected Boolean matchSuperClass(String superClassName) {
		return matchTargetType(superClassName);
	}

	@Override
	protected Boolean matchInterface(String interfaceName) {
		return matchTargetType(interfaceName);
	}

	protected Boolean matchTargetType(String typeName) {
		if (this.targetType.getName().equals(typeName)) {
			return true;
		}
		else if (Object.class.getName().equals(typeName)) {
			return Boolean.FALSE;
		}
		else if (typeName.startsWith("java.")) {
			try {
				Class<?> clazz = getClass().getClassLoader().loadClass(typeName);
				return Boolean.valueOf(this.targetType.isAssignableFrom(clazz));
			}
			catch (ClassNotFoundException ex) {
				// Class not found - can't determine a match that way.
			}
		}
		return null;
	}

}
