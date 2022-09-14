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

package org.springframework.aot.hint.predicate;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.springframework.aot.hint.ExecutableHint;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * Generator of {@link ReflectionHints} predicates, testing whether the given hints
 * match the expected behavior for reflection.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @since 6.0
 */
public class ReflectionHintsPredicates {

	ReflectionHintsPredicates() {
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the given type.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param typeReference the type
	 * @return the {@link RuntimeHints} predicate
	 */
	public TypeHintPredicate onType(TypeReference typeReference) {
		Assert.notNull(typeReference, "'typeReference' should not be null");
		return new TypeHintPredicate(typeReference);
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the given type.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param type the type
	 * @return the {@link RuntimeHints} predicate
	 */
	public TypeHintPredicate onType(Class<?> type) {
		Assert.notNull(type, "'type' should not be null");
		return new TypeHintPredicate(TypeReference.of(type));
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the given constructor.
	 * By default, both introspection and invocation hints match.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param constructor the constructor
	 * @return the {@link RuntimeHints} predicate
	 */
	public ConstructorHintPredicate onConstructor(Constructor<?> constructor) {
		Assert.notNull(constructor, "'constructor' should not be null");
		return new ConstructorHintPredicate(constructor);
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the given method.
	 * By default, both introspection and invocation hints match.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param method the method
	 * @return the {@link RuntimeHints} predicate
	 */
	public MethodHintPredicate onMethod(Method method) {
		Assert.notNull(method, "'method' should not be null");
		return new MethodHintPredicate(method);
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the method that matches the given selector.
	 * This looks up a method on the given type with the expected name, if unique.
	 * By default, both introspection and invocation hints match.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param type the type holding the method
	 * @param methodName the method name
	 * @return the {@link RuntimeHints} predicate
	 * @throws IllegalArgumentException if the method cannot be found or if multiple methods are found with the same name.
	 */
	public MethodHintPredicate onMethod(Class<?> type, String methodName) {
		Assert.notNull(type, "'type' should not be null");
		Assert.hasText(methodName, "'methodName' should not be null");
		return new MethodHintPredicate(getMethod(type, methodName));
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the method that matches the given selector.
	 * This looks up a method on the given type with the expected name, if unique.
	 * By default, both introspection and invocation hints match.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param className the name of the class holding the method
	 * @param methodName the method name
	 * @return the {@link RuntimeHints} predicate
	 * @throws ClassNotFoundException if the class cannot be resolved.
	 * @throws IllegalArgumentException if the method cannot be found or if multiple methods are found with the same name.
	 */
	public MethodHintPredicate onMethod(String className, String methodName) throws ClassNotFoundException {
		Assert.notNull(className, "'className' should not be null");
		Assert.hasText(methodName, "'methodName' should not be null");
		return onMethod(Class.forName(className), methodName);
	}

	private Method getMethod(Class<?> type, String methodName) {
		ReflectionUtils.MethodFilter selector = method -> methodName.equals(method.getName());
		Set<Method> methods = MethodIntrospector.selectMethods(type, selector);
		if (methods.size() == 1) {
			return methods.iterator().next();
		}
		else if (methods.size() > 1) {
			throw new IllegalArgumentException("Found multiple methods named '%s' on class %s".formatted(methodName, type.getName()));
		}
		else {
			throw new IllegalArgumentException("No method named '%s' on class %s".formatted(methodName, type.getName()));
		}
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the field that matches the given selector.
	 * This looks up a field on the given type with the expected name, if present.
	 * By default, unsafe or write access are not considered.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param type the type holding the field
	 * @param fieldName the field name
	 * @return the {@link RuntimeHints} predicate
	 * @throws IllegalArgumentException if a field cannot be found with the given name.
	 */
	public FieldHintPredicate onField(Class<?> type, String fieldName) {
		Assert.notNull(type, "'type' should not be null");
		Assert.hasText(fieldName, "'fieldName' should not be empty");
		Field field = ReflectionUtils.findField(type, fieldName);
		if (field == null) {
			throw new IllegalArgumentException("No field named '%s' on class %s".formatted(fieldName, type.getName()));
		}
		return new FieldHintPredicate(field);
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the field that matches the given selector.
	 * This looks up a field on the given type with the expected name, if present.
	 * By default, unsafe or write access are not considered.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param className the name of the class holding the field
	 * @param fieldName the field name
	 * @return the {@link RuntimeHints} predicate
	 * @throws ClassNotFoundException if the class cannot be resolved.
	 * @throws IllegalArgumentException if a field cannot be found with the given name.
	 */
	public FieldHintPredicate onField(String className, String fieldName) throws ClassNotFoundException {
		Assert.notNull(className, "'className' should not be null");
		Assert.hasText(fieldName, "'fieldName' should not be empty");
		return onField(Class.forName(className), fieldName);
	}

	/**
	 * Return a predicate that checks whether a reflection hint is registered for the given field.
	 * By default, unsafe or write access are not considered.
	 * <p>The returned type exposes additional methods that refine the predicate behavior.
	 * @param field the field
	 * @return the {@link RuntimeHints} predicate
	 */
	public FieldHintPredicate onField(Field field) {
		Assert.notNull(field, "'field' should not be null");
		return new FieldHintPredicate(field);
	}

	public static class TypeHintPredicate implements Predicate<RuntimeHints> {

		private final TypeReference type;

		TypeHintPredicate(TypeReference type) {
			this.type = type;
		}

		@Nullable
		private TypeHint getTypeHint(RuntimeHints hints) {
			return hints.reflection().getTypeHint(this.type);
		}

		@Override
		public boolean test(RuntimeHints hints) {
			return getTypeHint(hints) != null;
		}


		/**
		 * Refine the current predicate to only match if the given {@link MemberCategory} is present.
		 * @param memberCategory the member category
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public Predicate<RuntimeHints> withMemberCategory(MemberCategory memberCategory) {
			Assert.notNull(memberCategory, "'memberCategory' should not be null");
			return this.and(hints -> getTypeHint(hints).getMemberCategories().contains(memberCategory));
		}

		/**
		 * Refine the current predicate to only match if the given {@link MemberCategory categories} are present.
		 * @param memberCategories the member categories
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public Predicate<RuntimeHints> withMemberCategories(MemberCategory... memberCategories) {
			Assert.notEmpty(memberCategories, "'memberCategories' should not be empty");
			return this.and(hints -> getTypeHint(hints).getMemberCategories().containsAll(Arrays.asList(memberCategories)));
		}

		/**
		 * Refine the current predicate to match if any of the given {@link MemberCategory categories} is present.
		 * @param memberCategories the member categories
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public Predicate<RuntimeHints> withAnyMemberCategory(MemberCategory... memberCategories) {
			Assert.notEmpty(memberCategories, "'memberCategories' should not be empty");
			return this.and(hints -> Arrays.stream(memberCategories)
					.anyMatch(memberCategory -> getTypeHint(hints).getMemberCategories().contains(memberCategory)));
		}

		/**
		 * Refine the current predicate to only match if a hint is present for any of its constructors.
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public Predicate<RuntimeHints> withAnyConstructor() {
			return this.and(hints -> getTypeHint(hints).constructors().findAny().isPresent());
		}

		/**
		 * Refine the current predicate to only match if a hint is present for any of its methods.
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public Predicate<RuntimeHints> withAnyMethod() {
			return this.and(hints -> getTypeHint(hints).methods().findAny().isPresent());
		}

		/**
		 * Refine the current predicate to only match if a hint is present for any of its fields.
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public Predicate<RuntimeHints> withAnyField() {
			return this.and(hints -> getTypeHint(hints).fields().findAny().isPresent());
		}
	}

	public abstract static class ExecutableHintPredicate<T extends Executable> implements Predicate<RuntimeHints> {

		protected final T executable;

		protected ExecutableMode executableMode = ExecutableMode.INTROSPECT;

		ExecutableHintPredicate(T executable) {
			this.executable = executable;
		}

		/**
		 * Refine the current predicate to match for reflection introspection on the current type.
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public ExecutableHintPredicate<T> introspect() {
			this.executableMode = ExecutableMode.INTROSPECT;
			return this;
		}

		/**
		 * Refine the current predicate to match for reflection invocation on the current type.
		 * @return the refined {@link RuntimeHints} predicate
		 */
		public ExecutableHintPredicate<T> invoke() {
			this.executableMode = ExecutableMode.INVOKE;
			return this;
		}

		@Override
		public boolean test(RuntimeHints runtimeHints) {
			return (new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass()))
					.withAnyMemberCategory(getPublicMemberCategories())
					.and(hints -> Modifier.isPublic(this.executable.getModifiers())))
					.or(new TypeHintPredicate(TypeReference.of(this.executable.getDeclaringClass())).withAnyMemberCategory(getDeclaredMemberCategories()))
					.or(exactMatch()).test(runtimeHints);
		}

		abstract MemberCategory[] getPublicMemberCategories();

		abstract MemberCategory[] getDeclaredMemberCategories();

		abstract Predicate<RuntimeHints> exactMatch();

		/**
		 * Indicate whether the specified {@code ExecutableHint} covers the
		 * reflection needs of the specified executable definition.
		 * @return {@code true} if the member matches (same type, name, and parameters),
		 * and the configured {@code ExecutableMode} is compatible
		 */
		static boolean includes(ExecutableHint hint, String name,
				List<TypeReference> parameterTypes, ExecutableMode executableModes) {
			return hint.getName().equals(name) && hint.getParameterTypes().equals(parameterTypes) &&
					(hint.getMode().equals(ExecutableMode.INVOKE) || !executableModes.equals(ExecutableMode.INVOKE));
		}
	}

	public static class ConstructorHintPredicate extends ExecutableHintPredicate<Constructor<?>> {

		ConstructorHintPredicate(Constructor<?> constructor) {
			super(constructor);
		}

		@Override
		MemberCategory[] getPublicMemberCategories() {
			if (this.executableMode == ExecutableMode.INTROSPECT) {
				return new MemberCategory[] { MemberCategory.INTROSPECT_PUBLIC_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS };
			}
			return new MemberCategory[] { MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS };
		}

		@Override
		MemberCategory[] getDeclaredMemberCategories() {
			if (this.executableMode == ExecutableMode.INTROSPECT) {
				return new MemberCategory[] { MemberCategory.INTROSPECT_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_DECLARED_CONSTRUCTORS };
			}
			return new MemberCategory[] { MemberCategory.INVOKE_DECLARED_CONSTRUCTORS };
		}

		@Override
		Predicate<RuntimeHints> exactMatch() {
			return hints -> (hints.reflection().getTypeHint(this.executable.getDeclaringClass()) != null) &&
					hints.reflection().getTypeHint(this.executable.getDeclaringClass()).constructors().anyMatch(executableHint -> {
						List<TypeReference> parameters = TypeReference.listOf(this.executable.getParameterTypes());
						return includes(executableHint, "<init>", parameters, this.executableMode);
					});
		}

	}

	public static class MethodHintPredicate extends ExecutableHintPredicate<Method> {

		MethodHintPredicate(Method method) {
			super(method);
		}

		@Override
		MemberCategory[] getPublicMemberCategories() {
			if (this.executableMode == ExecutableMode.INTROSPECT) {
				return new MemberCategory[] { MemberCategory.INTROSPECT_PUBLIC_METHODS,
						MemberCategory.INVOKE_PUBLIC_METHODS };
			}
			return new MemberCategory[] { MemberCategory.INVOKE_PUBLIC_METHODS };
		}

		@Override
		MemberCategory[] getDeclaredMemberCategories() {

			if (this.executableMode == ExecutableMode.INTROSPECT) {
				return new MemberCategory[] { MemberCategory.INTROSPECT_DECLARED_METHODS,
						MemberCategory.INVOKE_DECLARED_METHODS };
			}
			return new MemberCategory[] { MemberCategory.INVOKE_DECLARED_METHODS };
		}

		@Override
		Predicate<RuntimeHints> exactMatch() {
			return hints -> (hints.reflection().getTypeHint(this.executable.getDeclaringClass()) != null) &&
					hints.reflection().getTypeHint(this.executable.getDeclaringClass()).methods().anyMatch(executableHint -> {
						List<TypeReference> parameters = TypeReference.listOf(this.executable.getParameterTypes());
						return includes(executableHint, this.executable.getName(), parameters, this.executableMode);
					});
		}

	}

	public static class FieldHintPredicate implements Predicate<RuntimeHints> {

		private final Field field;

		FieldHintPredicate(Field field) {
			this.field = field;
		}

		@Override
		public boolean test(RuntimeHints runtimeHints) {
			TypeHint typeHint = runtimeHints.reflection().getTypeHint(this.field.getDeclaringClass());
			if (typeHint == null) {
				return false;
			}
			return memberCategoryMatch(typeHint) || exactMatch(typeHint);
		}

		private boolean memberCategoryMatch(TypeHint typeHint) {
			if (Modifier.isPublic(this.field.getModifiers())) {
				return typeHint.getMemberCategories().contains(MemberCategory.PUBLIC_FIELDS) ||
						typeHint.getMemberCategories().contains(MemberCategory.DECLARED_FIELDS);
			}
			else {
				return typeHint.getMemberCategories().contains(MemberCategory.DECLARED_FIELDS);
			}
		}

		private boolean exactMatch(TypeHint typeHint) {
			return typeHint.fields().anyMatch(fieldHint ->
					this.field.getName().equals(fieldHint.getName()));
		}

	}

}
