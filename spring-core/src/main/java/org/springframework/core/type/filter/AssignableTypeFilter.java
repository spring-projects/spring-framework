/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.filter;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ClassUtils;

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

	/**
	 * Return the {@code type} that this instance is using to filter candidates.
	 * @since 5.0
	 */
	public final Class<?> getTargetType() {
		return this.targetType;
	}

	@Override
	protected boolean matchClassName(String className) {
		return this.targetType.getName().equals(className);
	}

	@Override
	protected @Nullable Boolean matchSuperClass(String superClassName) {
		return matchTargetType(superClassName);
	}

	@Override
	protected @Nullable Boolean matchInterface(String interfaceName) {
		return matchTargetType(interfaceName);
	}

	protected @Nullable Boolean matchTargetType(String typeName) {
		if (this.targetType.getName().equals(typeName)) {
			return true;
		}
		else if (Object.class.getName().equals(typeName)) {
			return false;
		}
		else if (typeName.startsWith("java")) {
			try {
				Class<?> clazz = ClassUtils.forName(typeName, getClass().getClassLoader());
				return this.targetType.isAssignableFrom(clazz);
			}
			catch (Throwable ex) {
				// Class not regularly loadable - can't determine a match that way.
			}
		}
		return null;
	}

}
