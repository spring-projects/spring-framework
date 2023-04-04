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

	/**
	 * Check if the right-hand side type may be assigned to the left-hand side
	 * type following the Java generics rules.
	 * @param leftHandSideType the target type
	 * @param rightHandSideType the value type that should be assigned to the target type
	 * @return true if right-hand side is assignable to left-hand side
	 */
	public static boolean isAssignable(Type leftHandSideType, Type rightHandSideType) {
		Assert.notNull(leftHandSideType, "Left-hand side type must not be null");
		Assert.notNull(rightHandSideType, "Right-hand side type must not be null");

		// all types are assignable to themselves and to class Object
		if (leftHandSideType.equals(rightHandSideType) || Object.class == leftHandSideType) {
			return true;
		}

		if (leftHandSideType instanceof Class<?> leftHandSideClass) {
			// just comparing two classes
			if (rightHandSideType instanceof Class<?> rightHandSideClass) {
				return ClassUtils.isAssignable(leftHandSideClass, rightHandSideClass);
			}

			if (rightHandSideType instanceof ParameterizedType rightHandSideParameterizedType) {
				Type rightHandSideRawType = rightHandSideParameterizedType.getRawType();

				// a parameterized type is always assignable to its raw class type
				if (rightHandSideRawType instanceof Class<?> rightHandSideRawClass) {
					return ClassUtils.isAssignable(leftHandSideClass, rightHandSideRawClass);
				}
			}
			else if (leftHandSideClass.isArray() && rightHandSideType instanceof GenericArrayType rightHandSideGenericArrayType) {
				Type rightHandSideComponent = rightHandSideGenericArrayType.getGenericComponentType();

				return isAssignable(leftHandSideClass.getComponentType(), rightHandSideComponent);
			}
		}

		// parameterized types are only assignable to other parameterized types and class types
		if (leftHandSideType instanceof ParameterizedType leftHandSideParameterizedType) {
			if (rightHandSideType instanceof Class<?> rightHandSideClass) {
				Type leftHandSideRawType = leftHandSideParameterizedType.getRawType();

				if (leftHandSideRawType instanceof Class<?> leftHandSideClass) {
					return ClassUtils.isAssignable(leftHandSideClass, rightHandSideClass);
				}
			}
			else if (rightHandSideType instanceof ParameterizedType rightHandSideParameterizedType) {
				return isAssignable(leftHandSideParameterizedType, rightHandSideParameterizedType);
			}
		}

		if (leftHandSideType instanceof GenericArrayType leftHandSideGenericArrayType) {
			Type leftHandSideComponent = leftHandSideGenericArrayType.getGenericComponentType();

			if (rightHandSideType instanceof Class<?> rightHandSideClass && rightHandSideClass.isArray()) {
				return isAssignable(leftHandSideComponent, rightHandSideClass.getComponentType());
			}
			else if (rightHandSideType instanceof GenericArrayType rightHandSideGenericArrayType) {
				Type rightHandSideComponent = rightHandSideGenericArrayType.getGenericComponentType();

				return isAssignable(leftHandSideComponent, rightHandSideComponent);
			}
		}

		if (leftHandSideType instanceof WildcardType leftHandSideWildcardType) {
			return isAssignable(leftHandSideWildcardType, rightHandSideType);
		}

		return false;
	}

	private static boolean isAssignable(ParameterizedType leftHandSideType, ParameterizedType rightHandSideType) {
		if (leftHandSideType.equals(rightHandSideType)) {
			return true;
		}

		Type[] leftHandSideTypeArguments = leftHandSideType.getActualTypeArguments();
		Type[] rightHandSideTypeArguments = rightHandSideType.getActualTypeArguments();

		if (leftHandSideTypeArguments.length != rightHandSideTypeArguments.length) {
			return false;
		}

		for (int size = leftHandSideTypeArguments.length, i = 0; i < size; ++i) {
			Type leftHandSideTypeArgument = leftHandSideTypeArguments[i];
			Type rightHandSideTypeArgument = rightHandSideTypeArguments[i];

			if (!leftHandSideTypeArgument.equals(rightHandSideTypeArgument) &&
					!(leftHandSideTypeArgument instanceof WildcardType wildcardType && isAssignable(wildcardType, rightHandSideTypeArgument))) {
				return false;
			}
		}

		return true;
	}

	private static boolean isAssignable(WildcardType leftHandSideType, Type rightHandSideType) {
		Type[] leftUpperBounds = getUpperBounds(leftHandSideType);

		Type[] leftLowerBounds = getLowerBounds(leftHandSideType);

		if (rightHandSideType instanceof WildcardType rightHandSideWildcardType) {
			// both the upper and lower bounds of the right-hand side must be
			// completely enclosed in the upper and lower bounds of the left-
			// hand side.
			Type[] rightUpperBounds = getUpperBounds(rightHandSideWildcardType);

			Type[] rightLowerBounds = getLowerBounds(rightHandSideWildcardType);

			for (Type leftBound : leftUpperBounds) {
				for (Type rightBound : rightUpperBounds) {
					if (!isAssignableBound(leftBound, rightBound)) {
						return false;
					}
				}

				for (Type rightBound : rightLowerBounds) {
					if (!isAssignableBound(leftBound, rightBound)) {
						return false;
					}
				}
			}

			for (Type leftBound : leftLowerBounds) {
				for (Type rightBound : rightUpperBounds) {
					if (!isAssignableBound(rightBound, leftBound)) {
						return false;
					}
				}

				for (Type rightBound : rightLowerBounds) {
					if (!isAssignableBound(rightBound, leftBound)) {
						return false;
					}
				}
			}
		}
		else {
			for (Type leftBound : leftUpperBounds) {
				if (!isAssignableBound(leftBound, rightHandSideType)) {
					return false;
				}
			}

			for (Type leftBound : leftLowerBounds) {
				if (!isAssignableBound(rightHandSideType, leftBound)) {
					return false;
				}
			}
		}

		return true;
	}

	private static Type[] getLowerBounds(WildcardType wildcardType) {
		Type[] lowerBounds = wildcardType.getLowerBounds();

		// supply the implicit lower bound if none are specified
		if (lowerBounds.length == 0) {
			lowerBounds = new Type[] { null };
		}
		return lowerBounds;
	}

	private static Type[] getUpperBounds(WildcardType wildcardType) {
		Type[] upperBounds = wildcardType.getUpperBounds();

		// supply the implicit upper bound if none are specified
		if (upperBounds.length == 0) {
			upperBounds = new Type[] { Object.class };
		}
		return upperBounds;
	}

	public static boolean isAssignableBound(@Nullable Type leftHandSideType, @Nullable Type rightHandSideType) {
		if (rightHandSideType == null) {
			return true;
		}
		if (leftHandSideType == null) {
			return false;
		}
		return isAssignable(leftHandSideType, rightHandSideType);
	}

}
