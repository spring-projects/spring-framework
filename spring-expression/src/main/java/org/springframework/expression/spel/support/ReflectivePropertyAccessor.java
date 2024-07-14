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

package org.springframework.expression.spel.support;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import kotlin.jvm.JvmClassMappingKt;
import kotlin.reflect.KClass;
import kotlin.reflect.KMutableProperty;
import kotlin.reflect.KProperty;
import kotlin.reflect.full.KClasses;
import kotlin.reflect.jvm.ReflectJvmMapping;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.KotlinDetector;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * A powerful {@link PropertyAccessor} that uses reflection to access properties
 * for reading and possibly also for writing on a target instance.
 *
 * <p>A property can be referenced through a public getter method (when being read)
 * or a public setter method (when being written), and also through a public field.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Sebastien Deleuze
 * @since 3.0
 * @see StandardEvaluationContext
 * @see SimpleEvaluationContext
 * @see DataBindingPropertyAccessor
 */
public class ReflectivePropertyAccessor implements PropertyAccessor {

	private static final Set<Class<?>> ANY_TYPES = Collections.emptySet();

	private static final Set<Class<?>> BOOLEAN_TYPES = Set.of(Boolean.class, boolean.class);

	private final boolean allowWrite;

	private final Map<PropertyCacheKey, InvokerPair> readerCache = new ConcurrentHashMap<>(64);

	private final Map<PropertyCacheKey, Member> writerCache = new ConcurrentHashMap<>(64);

	private final Map<PropertyCacheKey, TypeDescriptor> typeDescriptorCache = new ConcurrentHashMap<>(64);

	private final Map<Class<?>, Method[]> sortedMethodsCache = new ConcurrentHashMap<>(64);


	/**
	 * Create a new property accessor for reading as well as writing.
	 * @see #ReflectivePropertyAccessor(boolean)
	 */
	public ReflectivePropertyAccessor() {
		this(true);
	}

	/**
	 * Create a new property accessor for reading and possibly also writing.
	 * @param allowWrite whether to allow write operations on a target instance
	 * @since 4.3.15
	 * @see #canWrite
	 */
	public ReflectivePropertyAccessor(boolean allowWrite) {
		this.allowWrite = allowWrite;
	}


	/**
	 * Returns {@code null} which means this is a general purpose accessor.
	 */
	@Override
	@Nullable
	public Class<?>[] getSpecificTargetClasses() {
		return null;
	}

	@Override
	public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		if (target == null) {
			return false;
		}

		Class<?> type = (target instanceof Class<?> clazz ? clazz : target.getClass());
		if (type.isArray() && name.equals("length")) {
			return true;
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		if (this.readerCache.containsKey(cacheKey)) {
			return true;
		}

		Method method = findGetterForProperty(name, type, target);
		if (method != null) {
			// Treat it like a property...
			// The readerCache will only contain gettable properties (let's not worry about setters for now).
			Property property = new Property(type, method, null);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
			Method methodToInvoke = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, type);
			this.readerCache.put(cacheKey, new InvokerPair(methodToInvoke, typeDescriptor));
			this.typeDescriptorCache.put(cacheKey, typeDescriptor);
			return true;
		}
		else {
			Field field = findField(name, type, target);
			if (field != null) {
				TypeDescriptor typeDescriptor = new TypeDescriptor(field);
				this.readerCache.put(cacheKey, new InvokerPair(field, typeDescriptor));
				this.typeDescriptorCache.put(cacheKey, typeDescriptor);
				return true;
			}
		}

		return false;
	}

	@Override
	@SuppressWarnings("NullAway")
	public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		Assert.state(target != null, "Target must not be null");
		Class<?> type = (target instanceof Class<?> clazz ? clazz : target.getClass());

		if (type.isArray() && name.equals("length")) {
			if (target instanceof Class) {
				throw new AccessException("Cannot access length on array class itself");
			}
			return new TypedValue(Array.getLength(target));
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		InvokerPair invoker = this.readerCache.get(cacheKey);

		if (invoker == null || invoker.member instanceof Method) {
			Method method = (Method) (invoker != null ? invoker.member : null);
			Method methodToInvoke = method;
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					// Treat it like a property...
					// The readerCache will only contain gettable properties (let's not worry about setters for now).
					Property property = new Property(type, method, null);
					TypeDescriptor typeDescriptor = new TypeDescriptor(property);
					methodToInvoke = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, type);
					invoker = new InvokerPair(methodToInvoke, typeDescriptor);
					this.readerCache.put(cacheKey, invoker);
				}
			}
			if (methodToInvoke != null) {
				try {
					ReflectionUtils.makeAccessible(methodToInvoke);
					Object value = methodToInvoke.invoke(target);
					return new TypedValue(value, invoker.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
				}
			}
		}

		if (invoker == null || invoker.member instanceof Field) {
			Field field = (Field) (invoker == null ? null : invoker.member);
			if (field == null) {
				field = findField(name, type, target);
				if (field != null) {
					invoker = new InvokerPair(field, new TypeDescriptor(field));
					this.readerCache.put(cacheKey, invoker);
				}
			}
			if (field != null) {
				try {
					ReflectionUtils.makeAccessible(field);
					Object value = field.get(target);
					return new TypedValue(value, invoker.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		throw new AccessException("Neither getter method nor field found for property '" + name + "'");
	}

	@Override
	public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
		if (!this.allowWrite || target == null) {
			return false;
		}

		Class<?> type = (target instanceof Class<?> clazz ? clazz : target.getClass());
		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		if (this.writerCache.containsKey(cacheKey)) {
			return true;
		}

		Method method = findSetterForProperty(name, type, target);
		if (method != null) {
			// Treat it like a property
			Property property = new Property(type, null, method);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
			method = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, type);
			this.writerCache.put(cacheKey, method);
			this.typeDescriptorCache.put(cacheKey, typeDescriptor);
			return true;
		}
		else {
			Field field = findField(name, type, target);
			if (field != null) {
				this.writerCache.put(cacheKey, field);
				this.typeDescriptorCache.put(cacheKey, new TypeDescriptor(field));
				return true;
			}
		}

		return false;
	}

	@Override
	public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue)
			throws AccessException {

		if (!this.allowWrite) {
			throw new AccessException("PropertyAccessor for property '" + name +
					"' on target [" + target + "] does not allow write operations");
		}

		Assert.state(target != null, "Target must not be null");
		Class<?> type = (target instanceof Class<?> clazz ? clazz : target.getClass());

		Object possiblyConvertedNewValue = newValue;
		TypeDescriptor typeDescriptor = getTypeDescriptor(context, target, name);
		if (typeDescriptor != null) {
			try {
				possiblyConvertedNewValue = context.getTypeConverter().convertValue(
						newValue, TypeDescriptor.forObject(newValue), typeDescriptor);
			}
			catch (EvaluationException evaluationException) {
				throw new AccessException("Type conversion failure", evaluationException);
			}
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		Member cachedMember = this.writerCache.get(cacheKey);

		if (cachedMember == null || cachedMember instanceof Method) {
			Method method = (Method) cachedMember;
			if (method == null) {
				method = findSetterForProperty(name, type, target);
				if (method != null) {
					method = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, type);
					cachedMember = method;
					this.writerCache.put(cacheKey, cachedMember);
				}
			}
			if (method != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					method.invoke(target, possiblyConvertedNewValue);
					return;
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through setter method", ex);
				}
			}
		}

		if (cachedMember == null || cachedMember instanceof Field) {
			Field field = (Field) cachedMember;
			if (field == null) {
				field = findField(name, type, target);
				if (field != null) {
					cachedMember = field;
					this.writerCache.put(cacheKey, cachedMember);
				}
			}
			if (field != null) {
				try {
					ReflectionUtils.makeAccessible(field);
					field.set(target, possiblyConvertedNewValue);
					return;
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		throw new AccessException("Neither setter method nor field found for property '" + name + "'");
	}


	@Nullable
	private TypeDescriptor getTypeDescriptor(EvaluationContext context, Object target, String name) {
		Class<?> type = (target instanceof Class<?> clazz ? clazz : target.getClass());

		if (type.isArray() && name.equals("length")) {
			return TypeDescriptor.valueOf(int.class);
		}
		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		TypeDescriptor typeDescriptor = this.typeDescriptorCache.get(cacheKey);
		if (typeDescriptor == null) {
			// Attempt to populate the cache entry
			try {
				if (canRead(context, target, name) || canWrite(context, target, name)) {
					typeDescriptor = this.typeDescriptorCache.get(cacheKey);
				}
			}
			catch (AccessException ex) {
				// Continue with null type descriptor
			}
		}
		return typeDescriptor;
	}

	@Nullable
	private Method findGetterForProperty(String propertyName, Class<?> clazz, Object target) {
		boolean targetIsAClass = (target instanceof Class);
		Method method = findGetterForProperty(propertyName, clazz, targetIsAClass);
		if (method == null && targetIsAClass) {
			// Fallback for getter instance methods in java.lang.Class.
			method = findGetterForProperty(propertyName, Class.class, false);
		}
		return method;
	}

	@Nullable
	private Method findSetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findSetterForProperty(propertyName, clazz, target instanceof Class);
		// In contrast to findGetterForProperty(), we do not look for setters in
		// java.lang.Class as a fallback, since Class doesn't have any public setters.
		return method;
	}

	/**
	 * Find a getter method for the specified property.
	 */
	@Nullable
	protected Method findGetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				"get", clazz, mustBeStatic, 0, ANY_TYPES);
		if (method == null) {
			method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
					"is", clazz, mustBeStatic, 0, BOOLEAN_TYPES);
			if (method == null) {
				// Record-style plain accessor method, e.g. name()
				method = findMethodForProperty(new String[] {propertyName},
						"", clazz, mustBeStatic, 0, ANY_TYPES);
			}
		}
		return method;
	}

	/**
	 * Find a setter method for the specified property.
	 */
	@Nullable
	protected Method findSetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		return findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				"set", clazz, mustBeStatic, 1, ANY_TYPES);
	}

	@Nullable
	private Method findMethodForProperty(String[] methodSuffixes, String prefix, Class<?> clazz,
			boolean mustBeStatic, int numberOfParams, Set<Class<?>> requiredReturnTypes) {

		Method[] methods = getSortedMethods(clazz);
		for (String methodSuffix : methodSuffixes) {
			for (Method method : methods) {
				if (isCandidateForProperty(method, clazz) &&
						(method.getName().equals(prefix + methodSuffix) || isKotlinProperty(method, methodSuffix)) &&
						method.getParameterCount() == numberOfParams &&
						(!mustBeStatic || Modifier.isStatic(method.getModifiers())) &&
						(requiredReturnTypes.isEmpty() || requiredReturnTypes.contains(method.getReturnType()))) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Return class methods ordered with non-bridge methods appearing higher.
	 */
	private Method[] getSortedMethods(Class<?> clazz) {
		return this.sortedMethodsCache.computeIfAbsent(clazz, key -> {
			Method[] methods = key.getMethods();
			Arrays.sort(methods, (o1, o2) -> (o1.isBridge() == o2.isBridge() ? 0 : (o1.isBridge() ? 1 : -1)));
			return methods;
		});
	}

	/**
	 * Determine whether the given {@code Method} is a candidate for property access
	 * on an instance of the given target class.
	 * <p>The default implementation considers any method as a candidate, even for
	 * non-user-declared properties on the {@link Object} base class.
	 * @param method the Method to evaluate
	 * @param targetClass the concrete target class that is being introspected
	 * @since 4.3.15
	 */
	protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
		return true;
	}

	/**
	 * Return the method suffixes for a given property name. The default implementation
	 * uses JavaBean conventions with additional support for properties of the form 'xY'
	 * where the method 'getXY()' is used in preference to the JavaBean convention of
	 * 'getxY()'.
	 */
	protected String[] getPropertyMethodSuffixes(String propertyName) {
		String suffix = getPropertyMethodSuffix(propertyName);
		if (suffix.length() > 0 && Character.isUpperCase(suffix.charAt(0))) {
			return new String[] {suffix};
		}
		return new String[] {suffix, StringUtils.capitalize(suffix)};
	}

	/**
	 * Return the method suffix for a given property name. The default implementation
	 * uses JavaBean conventions.
	 */
	protected String getPropertyMethodSuffix(String propertyName) {
		if (propertyName.length() > 1 && Character.isUpperCase(propertyName.charAt(1))) {
			return propertyName;
		}
		return StringUtils.capitalize(propertyName);
	}

	@Nullable
	private Field findField(String name, Class<?> clazz, Object target) {
		Field field = findField(name, clazz, target instanceof Class);
		if (field == null && target instanceof Class) {
			field = findField(name, target.getClass(), false);
		}
		return field;
	}

	/**
	 * Find a field of a certain name on a specified class.
	 */
	@Nullable
	protected Field findField(String name, Class<?> clazz, boolean mustBeStatic) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers()))) {
				return field;
			}
		}
		// We'll search superclasses and implemented interfaces explicitly,
		// although it shouldn't be necessary - however, see SPR-10125.
		if (clazz.getSuperclass() != null) {
			Field field = findField(name, clazz.getSuperclass(), mustBeStatic);
			if (field != null) {
				return field;
			}
		}
		for (Class<?> implementedInterface : clazz.getInterfaces()) {
			Field field = findField(name, implementedInterface, mustBeStatic);
			if (field != null) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Attempt to create an optimized property accessor tailored for a property
	 * of a particular name on a particular class.
	 * <p>The general {@link ReflectivePropertyAccessor} will always work but is
	 * not optimal due to the need to look up which reflective member (method or
	 * field) to use each time {@link #read(EvaluationContext, Object, String)}
	 * is called.
	 * <p>This method will return this {@code ReflectivePropertyAccessor} instance
	 * if it is unable to build an optimized accessor.
	 * <p>Note: An optimized accessor is currently only usable for read attempts.
	 * Do not call this method if you need a read-write accessor.
	 */
	@SuppressWarnings("NullAway")
	public PropertyAccessor createOptimalAccessor(EvaluationContext context, @Nullable Object target, String name) {
		// Don't be clever for arrays or a null target...
		if (target == null) {
			return this;
		}
		Class<?> type = (target instanceof Class<?> clazz ? clazz : target.getClass());
		if (type.isArray()) {
			return this;
		}

		PropertyCacheKey cacheKey = new PropertyCacheKey(type, name, target instanceof Class);
		InvokerPair invokerPair = this.readerCache.get(cacheKey);

		if (invokerPair == null || invokerPair.member instanceof Method) {
			Method method = (Method) (invokerPair != null ? invokerPair.member : null);
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					TypeDescriptor typeDescriptor = new TypeDescriptor(new MethodParameter(method, -1));
					Method methodToInvoke = ClassUtils.getPubliclyAccessibleMethodIfPossible(method, type);
					ReflectionUtils.makeAccessible(methodToInvoke);
					invokerPair = new InvokerPair(methodToInvoke, typeDescriptor);
					this.readerCache.put(cacheKey, invokerPair);
				}
			}
			if (method != null) {
				return new OptimalPropertyAccessor(invokerPair);
			}
		}

		if (invokerPair == null || invokerPair.member instanceof Field) {
			Field field = (invokerPair != null ? (Field) invokerPair.member : null);
			if (field == null) {
				field = findField(name, type, target instanceof Class);
				if (field != null) {
					ReflectionUtils.makeAccessible(field);
					invokerPair = new InvokerPair(field, new TypeDescriptor(field));
					this.readerCache.put(cacheKey, invokerPair);
				}
			}
			if (field != null) {
				return new OptimalPropertyAccessor(invokerPair);
			}
		}

		return this;
	}

	private static boolean isKotlinProperty(Method method, String methodSuffix) {
		Class<?> clazz = method.getDeclaringClass();
		return KotlinDetector.isKotlinReflectPresent() &&
				KotlinDetector.isKotlinType(clazz) &&
				KotlinDelegate.isKotlinProperty(method, methodSuffix);
	}


	/**
	 * Captures the member (method/field) to call reflectively to access a property value
	 * and the type descriptor for the value returned by the reflective call.
	 */
	private record InvokerPair(Member member, TypeDescriptor typeDescriptor) {
	}

	private record PropertyCacheKey(Class<?> clazz, String property, boolean targetIsClass)
			implements Comparable<PropertyCacheKey> {

		@Override
		public int compareTo(PropertyCacheKey other) {
			int result = this.clazz.getName().compareTo(other.clazz.getName());
			if (result == 0) {
				result = this.property.compareTo(other.property);
			}
			return result;
		}
	}


	/**
	 * An optimized {@link CompilablePropertyAccessor} that will use reflection
	 * but only knows how to access a particular property on a particular class.
	 * <p>This is unlike the general {@link ReflectivePropertyAccessor} which
	 * manages a cache of methods and fields that may be invoked to access
	 * different properties on different classes.
	 * <p>This optimized accessor exists because looking up the appropriate
	 * reflective method or field on each read is not cheap.
	 */
	private static class OptimalPropertyAccessor implements CompilablePropertyAccessor {

		/**
		 * The member being accessed.
		 */
		private final Member member;

		private final TypeDescriptor typeDescriptor;


		OptimalPropertyAccessor(InvokerPair invokerPair) {
			this.member = invokerPair.member;
			this.typeDescriptor = invokerPair.typeDescriptor;
		}

		@Override
		@Nullable
		public Class<?>[] getSpecificTargetClasses() {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean canRead(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			if (target == null) {
				return false;
			}
			Class<?> type = (target instanceof Class<?> clazz ? clazz : target.getClass());
			if (type.isArray()) {
				return false;
			}

			if (this.member instanceof Method method) {
				String getterName = "get" + StringUtils.capitalize(name);
				if (getterName.equals(method.getName())) {
					return true;
				}
				getterName = "is" + StringUtils.capitalize(name);
				if (getterName.equals(method.getName())) {
					return true;
				}
			}
			return this.member.getName().equals(name);
		}

		@Override
		public TypedValue read(EvaluationContext context, @Nullable Object target, String name) throws AccessException {
			if (this.member instanceof Method method) {
				try {
					ReflectionUtils.makeAccessible(method);
					Object value = method.invoke(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter method", ex);
				}
			}
			else {
				Field field = (Field) this.member;
				try {
					ReflectionUtils.makeAccessible(field);
					Object value = field.get(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field '" + name + "'", ex);
				}
			}
		}

		@Override
		public boolean canWrite(EvaluationContext context, @Nullable Object target, String name) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public void write(EvaluationContext context, @Nullable Object target, String name, @Nullable Object newValue) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean isCompilable() {
			return (Modifier.isPublic(this.member.getModifiers()) &&
					Modifier.isPublic(this.member.getDeclaringClass().getModifiers()));
		}

		@Override
		public Class<?> getPropertyType() {
			if (this.member instanceof Method method) {
				return method.getReturnType();
			}
			else {
				return ((Field) this.member).getType();
			}
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			Class<?> publicDeclaringClass = this.member.getDeclaringClass();
			Assert.state(Modifier.isPublic(publicDeclaringClass.getModifiers()),
					() -> "Failed to find public declaring class for: " + this.member);

			String classDesc = publicDeclaringClass.getName().replace('.', '/');
			boolean isStatic = Modifier.isStatic(this.member.getModifiers());
			String descriptor = cf.lastDescriptor();

			if (!isStatic) {
				if (descriptor == null) {
					cf.loadTarget(mv);
				}
				if (descriptor == null || !classDesc.equals(descriptor.substring(1))) {
					mv.visitTypeInsn(CHECKCAST, classDesc);
				}
			}
			else {
				if (descriptor != null) {
					// A static field/method call will not consume what is on the stack, so
					// it needs to be popped off.
					mv.visitInsn(POP);
				}
			}

			if (this.member instanceof Method method) {
				boolean isInterface = publicDeclaringClass.isInterface();
				int opcode = (isStatic ? INVOKESTATIC : isInterface ? INVOKEINTERFACE : INVOKEVIRTUAL);
				mv.visitMethodInsn(opcode, classDesc, method.getName(),
						CodeFlow.createSignatureDescriptor(method), isInterface);
			}
			else {
				mv.visitFieldInsn((isStatic ? GETSTATIC : GETFIELD), classDesc, this.member.getName(),
						CodeFlow.toJvmDescriptor(((Field) this.member).getType()));
			}
		}
	}

	/**
	 * Inner class to avoid a hard dependency on Kotlin at runtime.
	 */
	private static class KotlinDelegate {

		public static boolean isKotlinProperty(Method method, String methodSuffix) {
			KClass<?> kClass = JvmClassMappingKt.getKotlinClass(method.getDeclaringClass());
			for (KProperty<?> property : KClasses.getMemberProperties(kClass)) {
				if (methodSuffix.equalsIgnoreCase(property.getName()) &&
						(method.equals(ReflectJvmMapping.getJavaGetter(property)) ||
								property instanceof KMutableProperty<?> mutableProperty &&
										method.equals(ReflectJvmMapping.getJavaSetter(mutableProperty)))) {
					return true;
				}
			}
			return false;
		}

	}

}
