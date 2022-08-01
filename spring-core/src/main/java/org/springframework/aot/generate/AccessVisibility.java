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

package org.springframework.aot.generate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntFunction;

import org.springframework.core.ResolvableType;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Access visibility types as determined by the <a href=
 * "https://docs.oracle.com/javase/tutorial/java/javaOO/accesscontrol.html">modifiers</a>
 * on a {@link Member} or {@link ResolvableType}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see #forMember(Member)
 * @see #forResolvableType(ResolvableType)
 */
public enum AccessVisibility {

	/**
	 * Public visibility. The member or type is visible to all classes.
	 */
	PUBLIC,

	/**
	 * Protected visibility. The member or type is only visible to subclasses.
	 */
	PROTECTED,

	/**
	 * Package-private visibility. The member or type is only visible to classes
	 * in the same package.
	 */
	PACKAGE_PRIVATE,

	/**
	 * Private visibility. The member or type is not visible to other classes.
	 */
	PRIVATE;


	/**
	 * Determine the {@link AccessVisibility} for the given member. This method
	 * will consider the member modifier, parameter types, return types and any
	 * enclosing classes. The lowest overall visibility will be returned.
	 * @param member the source member
	 * @return the {@link AccessVisibility} for the member
	 */
	public static AccessVisibility forMember(Member member) {
		Assert.notNull(member, "'member' must not be null");
		AccessVisibility visibility = forModifiers(member.getModifiers());
		AccessVisibility declaringClassVisibility = forClass(member.getDeclaringClass());
		visibility = lowest(visibility, declaringClassVisibility);
		if (visibility != PRIVATE) {
			if (member instanceof Field field) {
				AccessVisibility fieldVisibility = forResolvableType(
						ResolvableType.forField(field));
				return lowest(visibility, fieldVisibility);
			}
			if (member instanceof Constructor<?> constructor) {
				AccessVisibility parameterVisibility = forParameterTypes(constructor,
						i -> ResolvableType.forConstructorParameter(constructor, i));
				return lowest(visibility, parameterVisibility);
			}
			if (member instanceof Method method) {
				AccessVisibility parameterVisibility = forParameterTypes(method,
						i -> ResolvableType.forMethodParameter(method, i));
				AccessVisibility returnTypeVisibility = forResolvableType(
						ResolvableType.forMethodReturnType(method));
				return lowest(visibility, parameterVisibility, returnTypeVisibility);
			}
		}
		return PRIVATE;
	}

	/**
	 * Determine the {@link AccessVisibility} for the given
	 * {@link ResolvableType}. This method will consider the type itself as well
	 * as any generics.
	 * @param resolvableType the source resolvable type
	 * @return the {@link AccessVisibility} for the type
	 */
	public static AccessVisibility forResolvableType(ResolvableType resolvableType) {
		return forResolvableType(resolvableType, new HashSet<>());
	}

	private static AccessVisibility forResolvableType(ResolvableType resolvableType,
			Set<ResolvableType> seen) {
		if (!seen.add(resolvableType)) {
			return AccessVisibility.PUBLIC;
		}
		Class<?> userClass = ClassUtils.getUserClass(resolvableType.toClass());
		ResolvableType userType = resolvableType.as(userClass);
		AccessVisibility visibility = forClass(userType.toClass());
		for (ResolvableType generic : userType.getGenerics()) {
			visibility = lowest(visibility, forResolvableType(generic, seen));
		}
		return visibility;
	}

	private static AccessVisibility forParameterTypes(Executable executable,
			IntFunction<ResolvableType> resolvableTypeFactory) {
		AccessVisibility visibility = AccessVisibility.PUBLIC;
		Class<?>[] parameterTypes = executable.getParameterTypes();
		for (int i = 0; i < parameterTypes.length; i++) {
			ResolvableType type = resolvableTypeFactory.apply(i);
			visibility = lowest(visibility, forResolvableType(type));
		}
		return visibility;
	}

	/**
	 * Determine the {@link AccessVisibility} for the given {@link Class}.
	 * @param clazz the source class
	 * @return the {@link AccessVisibility} for the class
	 */
	public static AccessVisibility forClass(Class<?> clazz) {
		clazz = ClassUtils.getUserClass(clazz);
		AccessVisibility visibility = forModifiers(clazz.getModifiers());
		if (clazz.isArray()) {
			visibility = lowest(visibility, forClass(clazz.getComponentType()));
		}
		Class<?> enclosingClass = clazz.getEnclosingClass();
		if (enclosingClass != null) {
			visibility = lowest(visibility, forClass(clazz.getEnclosingClass()));
		}
		return visibility;
	}

	private static AccessVisibility forModifiers(int modifiers) {
		if (Modifier.isPublic(modifiers)) {
			return PUBLIC;
		}
		if (Modifier.isProtected(modifiers)) {
			return PROTECTED;
		}
		if (Modifier.isPrivate(modifiers)) {
			return PRIVATE;
		}
		return PACKAGE_PRIVATE;
	}

	/**
	 * Returns the lowest {@link AccessVisibility} put of the given candidates.
	 * @param candidates the candidates to check
	 * @return the lowest {@link AccessVisibility} from the candidates
	 */
	public static AccessVisibility lowest(AccessVisibility... candidates) {
		AccessVisibility visibility = PUBLIC;
		for (AccessVisibility candidate : candidates) {
			if (candidate.ordinal() > visibility.ordinal()) {
				visibility = candidate;
			}
		}
		return visibility;
	}

}
