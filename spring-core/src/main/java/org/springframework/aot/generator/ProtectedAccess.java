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

package org.springframework.aot.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Gather the need of non-public access and determine the privileged package
 * to use, if necessary.
 *
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ProtectedAccess {

	private final List<ProtectedElement> elements;

	public ProtectedAccess() {
		this.elements = new ArrayList<>();
	}

	/**
	 * Specify whether the protected elements registered in this instance are
	 * accessible from the specified package name.
	 * @param packageName the target package name
	 * @return {@code true} if the registered access can be safely used from
	 * the specified package name
	 */
	public boolean isAccessible(String packageName) {
		return getProtectedElements(packageName).isEmpty();
	}

	/**
	 * Return the privileged package name to use for the specified package
	 * name, or {@code null} if none is required.
	 * @param packageName the target package name to use
	 * @return the privileged package name to use, or {@code null}
	 * @throws ProtectedAccessException if a single privileged package cannot
	 * be identified
	 * @see #isAccessible(String)
	 */
	@Nullable
	public String getPrivilegedPackageName(String packageName) throws ProtectedAccessException {
		List<ProtectedElement> protectedElements = getProtectedElements(packageName);
		if (protectedElements.isEmpty()) {
			return null;
		}
		List<String> packageNames = protectedElements.stream()
				.map(element -> element.getType().getPackageName())
				.distinct().toList();
		if (packageNames.size() == 1) {
			return packageNames.get(0);
		}
		throw new ProtectedAccessException("Multiple packages require a privileged access: "
				+ packageNames, protectedElements);
	}

	private List<ProtectedElement> getProtectedElements(String packageName) {
		List<ProtectedElement> matches = new ArrayList<>();
		for (ProtectedElement element : this.elements) {
			if (!element.getType().getPackage().getName().equals(packageName)) {
				matches.add(element);
			}
		}
		return matches;
	}

	/**
	 * Analyze the specified {@linkplain ResolvableType type}, including its
	 * full type signature.
	 * @param type the type to analyze
	 */
	public void analyze(ResolvableType type) {
		Class<?> protectedType = isProtected(type);
		if (protectedType != null) {
			registerProtectedType(protectedType, null);
		}
	}

	/**
	 * Analyze accessing the specified {@link Member} using the specified
	 * {@link Options options}.
	 * @param member the member to analyze
	 * @param options the options to use
	 */
	public void analyze(Member member, Options options) {
		if (isProtected(member.getDeclaringClass())) {
			registerProtectedType(member.getDeclaringClass(), member);
		}
		if (isProtected(member.getModifiers()) && !options.useReflection.apply(member)) {
			registerProtectedType(member.getDeclaringClass(), member);
		}
		if (member instanceof Field field) {
			ResolvableType fieldType = ResolvableType.forField(field);
			Class<?> protectedType = isProtected(fieldType);
			if (protectedType != null && options.assignReturnType.apply(field)) {
				registerProtectedType(protectedType, field);
			}
		}
		else if (member instanceof Constructor<?> constructor) {
			analyzeParameterTypes(constructor, i ->
					ResolvableType.forConstructorParameter(constructor, i));
		}
		else if (member instanceof Method method) {
			ResolvableType returnType = ResolvableType.forMethodReturnType(method);
			Class<?> protectedType = isProtected(returnType);
			if (protectedType != null && options.assignReturnType.apply(method)) {
				registerProtectedType(protectedType, method);
			}
			analyzeParameterTypes(method, i -> ResolvableType.forMethodParameter(method, i));
		}
	}

	private void analyzeParameterTypes(Executable executable, Function<Integer,
			ResolvableType> parameterTypeFactory) {

		for (int i = 0; i < executable.getParameters().length; i++) {
			ResolvableType parameterType = parameterTypeFactory.apply(i);
			Class<?> protectedType = isProtected(parameterType);
			if (protectedType != null) {
				registerProtectedType(protectedType, executable);
			}
		}
	}

	@Nullable
	Class<?> isProtected(ResolvableType resolvableType) {
		return isProtected(new HashSet<>(), resolvableType);
	}

	@Nullable
	private Class<?> isProtected(Set<ResolvableType> seen, ResolvableType target) {
		if (seen.contains(target)) {
			return null;
		}
		seen.add(target);
		ResolvableType nonProxyTarget = target.as(ClassUtils.getUserClass(target.toClass()));
		Class<?> rawClass = nonProxyTarget.toClass();
		if (isProtected(rawClass)) {
			return rawClass;
		}
		Class<?> declaringClass = rawClass.getDeclaringClass();
		if (declaringClass != null) {
			if (isProtected(declaringClass)) {
				return declaringClass;
			}
		}
		if (nonProxyTarget.hasGenerics()) {
			for (ResolvableType generic : nonProxyTarget.getGenerics()) {
				return isProtected(seen, generic);
			}
		}
		return null;
	}

	private boolean isProtected(Class<?> type) {
		Class<?> candidate = ClassUtils.getUserClass(type);
		return isProtected(candidate.getModifiers());
	}

	private boolean isProtected(int modifiers) {
		return !Modifier.isPublic(modifiers);
	}

	private void registerProtectedType(Class<?> type, @Nullable Member member) {
		this.elements.add(ProtectedElement.of(type, member));
	}

	/**
	 * Options to use to analyze if invoking a {@link Member} requires
	 * privileged access.
	 */
	public static final class Options {

		private final Function<Member, Boolean> assignReturnType;

		private final Function<Member, Boolean> useReflection;


		private Options(Builder builder) {
			this.assignReturnType = builder.assignReturnType;
			this.useReflection = builder.useReflection;
		}

		/**
		 * Initialize a {@link Builder} with default options, that is use
		 * reflection if the member is private and does not assign the
		 * return type.
		 * @return an options builder
		 */
		public static Builder defaults() {
			return new Builder(member -> false,
					member -> Modifier.isPrivate(member.getModifiers()));
		}

		public static final class Builder {

			private Function<Member, Boolean> assignReturnType;

			private Function<Member, Boolean> useReflection;

			private Builder(Function<Member, Boolean> assignReturnType,
					Function<Member, Boolean> useReflection) {
				this.assignReturnType = assignReturnType;
				this.useReflection = useReflection;
			}

			/**
			 * Specify if the return type is assigned so that its type can be
			 * analyzed if necessary.
			 * @param assignReturnType whether the return type is assigned
			 * @return {@code this}, to facilitate method chaining
			 */
			public Builder assignReturnType(boolean assignReturnType) {
				return assignReturnType(member -> assignReturnType);
			}

			/**
			 * Specify a function that determines whether the return type is
			 * assigned so that its type can be analyzed.
			 * @param assignReturnType whether the return type is assigned
			 * @return {@code this}, to facilitate method chaining
			 */
			public Builder assignReturnType(Function<Member, Boolean> assignReturnType) {
				this.assignReturnType = assignReturnType;
				return this;
			}

			/**
			 * Specify a function that determines whether reflection can be
			 * used for a given {@link Member}.
			 * @param useReflection whether reflection can be used
			 * @return {@code this}, to facilitate method chaining
			 */
			public Builder useReflection(Function<Member, Boolean> useReflection) {
				this.useReflection = useReflection;
				return this;
			}

			/**
			 * Build an {@link Options} instance based on the state of this
			 * builder.
			 * @return a new options instance
			 */
			public Options build() {
				return new Options(this);
			}

		}

	}

}
