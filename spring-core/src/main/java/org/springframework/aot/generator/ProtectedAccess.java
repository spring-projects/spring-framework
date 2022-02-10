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
 * Gather the need of non-public access and determine the priviledged package
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
				.map(element -> element.getType().toClass().getPackageName())
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
			if (!element.getType().toClass().getPackage().getName().equals(packageName)) {
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
		if (isProtected(type)) {
			registerProtectedType(type, null);
		}
	}

	/**
	 * Analyze accessing the specified {@link Member} using the default
	 * {@linkplain Options#DEFAULTS options}.
	 * @param member the member to analyze
	 */
	public void analyze(Member member) {
		analyze(member, Options.DEFAULTS);
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
		if (!options.useReflection && isProtected(member.getModifiers())) {
			registerProtectedType(member.getDeclaringClass(), member);
		}
		if (member instanceof Field field) {
			ResolvableType fieldType = ResolvableType.forField(field);
			if (options.assignReturnType && isProtected(fieldType)) {
				registerProtectedType(fieldType, field);
			}
		}
		else if (member instanceof Constructor<?> constructor) {
			analyzeParameterTypes(constructor, i ->
					ResolvableType.forConstructorParameter(constructor, i));
		}
		else if (member instanceof Method method) {
			ResolvableType returnType = ResolvableType.forMethodReturnType(method);
			if (!options.assignReturnType && isProtected(returnType)) {
				registerProtectedType(returnType, method);
			}
			analyzeParameterTypes(method, i -> ResolvableType.forMethodParameter(method, i));
		}
	}

	private void analyzeParameterTypes(Executable executable, Function<Integer,
			ResolvableType> parameterTypeFactory) {

		for (int i = 0; i < executable.getParameters().length; i++) {
			ResolvableType parameterType = parameterTypeFactory.apply(i);
			if (isProtected(parameterType)) {
				registerProtectedType(parameterType, executable);
			}
		}
	}

	boolean isProtected(ResolvableType resolvableType) {
		return isProtected(new HashSet<>(), resolvableType);
	}

	private boolean isProtected(Set<ResolvableType> seen, ResolvableType target) {
		if (seen.contains(target)) {
			return false;
		}
		seen.add(target);
		ResolvableType nonProxyTarget = target.as(ClassUtils.getUserClass(target.toClass()));
		if (isProtected(nonProxyTarget.toClass())) {
			return true;
		}
		Class<?> declaringClass = nonProxyTarget.toClass().getDeclaringClass();
		if (declaringClass != null) {
			if (isProtected(declaringClass)) {
				return true;
			}
		}
		if (nonProxyTarget.hasGenerics()) {
			for (ResolvableType generic : nonProxyTarget.getGenerics()) {
				return isProtected(seen, generic);
			}
		}
		return false;
	}

	private boolean isProtected(Class<?> type) {
		Class<?> candidate = ClassUtils.getUserClass(type);
		return isProtected(candidate.getModifiers());
	}

	private boolean isProtected(int modifiers) {
		return !Modifier.isPublic(modifiers);
	}

	private void registerProtectedType(ResolvableType type, @Nullable Member member) {
		this.elements.add(ProtectedElement.of(type, member));
	}

	private void registerProtectedType(Class<?> type, Member member) {
		registerProtectedType(ResolvableType.forClass(type), member);
	}

	/**
	 * Options to use to analyze if invoking a {@link Member} requires
	 * privileged access.
	 */
	public static class Options {

		/**
		 * Default options that does fallback to reflection and does not
		 * assign the default type.
		 */
		public static final Options DEFAULTS = new Options();

		private final boolean useReflection;

		private final boolean assignReturnType;

		/**
		 * Create a new instance with the specified options.
		 * @param useReflection whether the writer can automatically use
		 * reflection to invoke a protected member if it is not public
		 * @param assignReturnType whether the writer needs to assign the
		 * return type, or if it is irrelevant
		 */
		public Options(boolean useReflection, boolean assignReturnType) {
			this.useReflection = useReflection;
			this.assignReturnType = assignReturnType;
		}

		private Options() {
			this(true, false);
		}

	}

}
