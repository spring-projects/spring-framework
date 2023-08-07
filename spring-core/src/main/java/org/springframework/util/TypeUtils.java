/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import org.springframework.lang.Nullable;

/**
 * Utility to work with generic type parameters.
 *
 * <p>Mainly for internal use within the framework.
 *
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 * @since 2.0.7
 */
public abstract class TypeUtils {

	private static final Type[] IMPLICIT_LOWER_BOUNDS = { null };

	private static final Type[] IMPLICIT_UPPER_BOUNDS = { Object.class };

	/**
	 * Check if the right-hand side type may be assigned to the left-hand side
	 * type following the Java generics rules.
	 * @param lhsType the target type (left-hand side (LHS) type)
	 * @param rhsType the value type (right-hand side (RHS) type) that should
	 * be assigned to the target type
	 * @return {@code true} if {@code rhsType} is assignable to {@code lhsType}
	 * @see ClassUtils#isAssignable(Class, Class)
	 */
	public static boolean isAssignable(Type lhsType, Type rhsType) {
		Assert.notNull(lhsType, "Left-hand side type must not be null");
		Assert.notNull(rhsType, "Right-hand side type must not be null");

		// all types are assignable to themselves and to class Object
		if (lhsType.equals(rhsType) || Object.class == lhsType) {
			return true;
		}

		if (lhsType instanceof Class<?> lhsClass) {
			// just comparing two classes
			if (rhsType instanceof Class<?> rhsClass) {
				return ClassUtils.isAssignable(lhsClass, rhsClass);
			}

			if (rhsType instanceof ParameterizedType rhsParameterizedType) {
				Type rhsRaw = rhsParameterizedType.getRawType();

				// a parameterized type is always assignable to its raw class type
				if (rhsRaw instanceof Class<?> rhRawClass) {
					return ClassUtils.isAssignable(lhsClass, rhRawClass);
				}
			}
			else if (lhsClass.isArray() && rhsType instanceof GenericArrayType rhsGenericArrayType) {
				Type rhsComponent = rhsGenericArrayType.getGenericComponentType();

				return isAssignable(lhsClass.componentType(), rhsComponent);
			}
		}

		// parameterized types are only assignable to other parameterized types and class types
		if (lhsType instanceof ParameterizedType lhsParameterizedType) {
			if (rhsType instanceof Class<?> rhsClass) {
				Type lhsRaw = lhsParameterizedType.getRawType();

				if (lhsRaw instanceof Class<?> lhsClass) {
					return ClassUtils.isAssignable(lhsClass, rhsClass);
				}
			}
			else if (rhsType instanceof ParameterizedType rhsParameterizedType) {
				return isAssignable(lhsParameterizedType, rhsParameterizedType);
			}
		}

		if (lhsType instanceof GenericArrayType lhsGenericArrayType) {
			Type lhsComponent = lhsGenericArrayType.getGenericComponentType();

			if (rhsType instanceof Class<?> rhsClass && rhsClass.isArray()) {
				return isAssignable(lhsComponent, rhsClass.componentType());
			}
			else if (rhsType instanceof GenericArrayType rhsGenericArrayType) {
				Type rhsComponent = rhsGenericArrayType.getGenericComponentType();

				return isAssignable(lhsComponent, rhsComponent);
			}
		}

		if (lhsType instanceof WildcardType lhsWildcardType) {
			return isAssignable(lhsWildcardType, rhsType);
		}

		return false;
	}

	private static boolean isAssignable(ParameterizedType lhsType, ParameterizedType rhsType) {
		if (lhsType.equals(rhsType)) {
			return true;
		}

		Type[] lhsTypeArguments = lhsType.getActualTypeArguments();
		Type[] rhsTypeArguments = rhsType.getActualTypeArguments();

		if (lhsTypeArguments.length != rhsTypeArguments.length) {
			return false;
		}

		for (int size = lhsTypeArguments.length, i = 0; i < size; ++i) {
			Type lhsArg = lhsTypeArguments[i];
			Type rhsArg = rhsTypeArguments[i];

			if (!lhsArg.equals(rhsArg) &&
					!(lhsArg instanceof WildcardType wildcardType && isAssignable(wildcardType, rhsArg))) {
				return false;
			}
		}

		return true;
	}

	private static boolean isAssignable(WildcardType lhsType, Type rhsType) {
		Type[] lUpperBounds = getUpperBounds(lhsType);

		Type[] lLowerBounds = getLowerBounds(lhsType);

		if (rhsType instanceof WildcardType rhsWcType) {
			// both the upper and lower bounds of the right-hand side must be
			// completely enclosed in the upper and lower bounds of the left-
			// hand side.
			Type[] rUpperBounds = getUpperBounds(rhsWcType);

			Type[] rLowerBounds = getLowerBounds(rhsWcType);

			for (Type lBound : lUpperBounds) {
				for (Type rBound : rUpperBounds) {
					if (!isAssignableBound(lBound, rBound)) {
						return false;
					}
				}

				for (Type rBound : rLowerBounds) {
					if (!isAssignableBound(lBound, rBound)) {
						return false;
					}
				}
			}

			for (Type lBound : lLowerBounds) {
				for (Type rBound : rUpperBounds) {
					if (!isAssignableBound(rBound, lBound)) {
						return false;
					}
				}

				for (Type rBound : rLowerBounds) {
					if (!isAssignableBound(rBound, lBound)) {
						return false;
					}
				}
			}
		}
		else {
			for (Type lBound : lUpperBounds) {
				if (!isAssignableBound(lBound, rhsType)) {
					return false;
				}
			}

			for (Type lBound : lLowerBounds) {
				if (!isAssignableBound(rhsType, lBound)) {
					return false;
				}
			}
		}

		return true;
	}

	private static Type[] getLowerBounds(WildcardType wildcardType) {
		Type[] lowerBounds = wildcardType.getLowerBounds();

		// supply the implicit lower bound if none are specified
		return (lowerBounds.length == 0 ? IMPLICIT_LOWER_BOUNDS : lowerBounds);
	}

	private static Type[] getUpperBounds(WildcardType wildcardType) {
		Type[] upperBounds = wildcardType.getUpperBounds();

		// supply the implicit upper bound if none are specified
		return (upperBounds.length == 0 ? IMPLICIT_UPPER_BOUNDS : upperBounds);
	}

	public static boolean isAssignableBound(@Nullable Type lhsType, @Nullable Type rhsType) {
		if (rhsType == null) {
			return true;
		}
		if (lhsType == null) {
			return false;
		}
		return isAssignable(lhsType, rhsType);
	}

}
