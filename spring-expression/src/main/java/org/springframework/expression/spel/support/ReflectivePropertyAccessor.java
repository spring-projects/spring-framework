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

package org.springframework.expression.spel.support;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.style.ToStringCreator;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.CompilablePropertyAccessor;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple {@link PropertyAccessor} that uses reflection to access properties
 * for reading and writing.
 *
 * <p>A property can be accessed through a public getter method (when being read)
 * or a public setter method (when being written), and also as a public field.
 *
 * @author Andy Clement
 * @author Juergen Hoeller
 * @author Phillip Webb
 * @since 3.0
 */
public class ReflectivePropertyAccessor implements PropertyAccessor {

	private static final Set<Class<?>> BOOLEAN_TYPES;
	static {
		Set<Class<?>> booleanTypes = new HashSet<Class<?>>();
		booleanTypes.add(Boolean.class);
		booleanTypes.add(Boolean.TYPE);
		BOOLEAN_TYPES = Collections.unmodifiableSet(booleanTypes);
	}

	private static final Set<Class<?>> ANY_TYPES = Collections.emptySet();


	private final Map<CacheKey, InvokerPair> readerCache = new ConcurrentHashMap<CacheKey, InvokerPair>(64);

	private final Map<CacheKey, Member> writerCache = new ConcurrentHashMap<CacheKey, Member>(64);

	private final Map<CacheKey, TypeDescriptor> typeDescriptorCache = new ConcurrentHashMap<CacheKey, TypeDescriptor>(64);

	private InvokerPair lastReadInvokerPair;


	/**
	 * Returns {@code null} which means this is a general purpose accessor.
	 */
	@Override
	public Class<?>[] getSpecificTargetClasses() {
		return null;
	}

	@Override
	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		if (target == null) {
			return false;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (type.isArray() && name.equals("length")) {
			return true;
		}
		CacheKey cacheKey = new CacheKey(type, name, target instanceof Class);
		if (this.readerCache.containsKey(cacheKey)) {
			return true;
		}
		Method method = findGetterForProperty(name, type, target);
		if (method != null) {
			// Treat it like a property...
			// The readerCache will only contain gettable properties (let's not worry about setters for now).
			Property property = new Property(type, method, null);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
			this.readerCache.put(cacheKey, new InvokerPair(method, typeDescriptor));
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

	public Member getLastReadInvokerPair() {
		return lastReadInvokerPair.member;
	}

	@Override
	public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
		if (target == null) {
			throw new AccessException("Cannot read property of null target");
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		if (type.isArray() && name.equals("length")) {
			if (target instanceof Class) {
				throw new AccessException("Cannot access length on array class itself");
			}
			return new TypedValue(Array.getLength(target));
		}

		CacheKey cacheKey = new CacheKey(type, name, target instanceof Class);
		InvokerPair invoker = this.readerCache.get(cacheKey);
		lastReadInvokerPair = invoker;

		if (invoker == null || invoker.member instanceof Method) {
			Method method = (Method) (invoker != null ? invoker.member : null);
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					// TODO remove the duplication here between canRead and read
					// Treat it like a property...
					// The readerCache will only contain gettable properties (let's not worry about setters for now).
					Property property = new Property(type, method, null);
					TypeDescriptor typeDescriptor = new TypeDescriptor(property);
					invoker = new InvokerPair(method, typeDescriptor);
					lastReadInvokerPair = invoker;
					this.readerCache.put(cacheKey, invoker);
				}
			}
			if (method != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					Object value = method.invoke(target);
					return new TypedValue(value, invoker.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter", ex);
				}
			}
		}

		if (invoker == null || invoker.member instanceof Field) {
			Field field = (Field) (invoker == null ? null : invoker.member);
			if (field == null) {
				field = findField(name, type, target);
				if (field != null) {
					invoker = new InvokerPair(field, new TypeDescriptor(field));
					lastReadInvokerPair = invoker;
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
					throw new AccessException("Unable to access field: " + name, ex);
				}
			}
		}

		throw new AccessException("Neither getter nor field found for property '" + name + "'");
	}

	@Override
	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		if (target == null) {
			return false;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		CacheKey cacheKey = new CacheKey(type, name, target instanceof Class);
		if (this.writerCache.containsKey(cacheKey)) {
			return true;
		}
		Method method = findSetterForProperty(name, type, target);
		if (method != null) {
			// Treat it like a property
			Property property = new Property(type, null, method);
			TypeDescriptor typeDescriptor = new TypeDescriptor(property);
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
	public void write(EvaluationContext context, Object target, String name, Object newValue) throws AccessException {
		if (target == null) {
			throw new AccessException("Cannot write property on null target");
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		Object possiblyConvertedNewValue = newValue;
		TypeDescriptor typeDescriptor = getTypeDescriptor(context, target, name);
		if (typeDescriptor != null) {
			try {
				possiblyConvertedNewValue = context.getTypeConverter().convertValue(
						newValue, TypeDescriptor.forObject(newValue), typeDescriptor);
			}
			catch (EvaluationException evaluationException) {
				throw new AccessException("Type conversion failure",evaluationException);
			}
		}
		CacheKey cacheKey = new CacheKey(type, name, target instanceof Class);
		Member cachedMember = this.writerCache.get(cacheKey);

		if (cachedMember == null || cachedMember instanceof Method) {
			Method method = (Method) cachedMember;
			if (method == null) {
				method = findSetterForProperty(name, type, target);
				if (method != null) {
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
					throw new AccessException("Unable to access property '" + name + "' through setter", ex);
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
					throw new AccessException("Unable to access field: " + name, ex);
				}
			}
		}

		throw new AccessException("Neither setter nor field found for property '" + name + "'");
	}

	private TypeDescriptor getTypeDescriptor(EvaluationContext context, Object target, String name) {
		if (target == null) {
			return null;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());

		if (type.isArray() && name.equals("length")) {
			return TypeDescriptor.valueOf(Integer.TYPE);
		}
		CacheKey cacheKey = new CacheKey(type, name, target instanceof Class);
		TypeDescriptor typeDescriptor = this.typeDescriptorCache.get(cacheKey);
		if (typeDescriptor == null) {
			// attempt to populate the cache entry
			try {
				if (canRead(context, target, name)) {
					typeDescriptor = this.typeDescriptorCache.get(cacheKey);
				}
				else if (canWrite(context, target, name)) {
					typeDescriptor = this.typeDescriptorCache.get(cacheKey);
				}
			}
			catch (AccessException ex) {
				// continue with null type descriptor
			}
		}
		return typeDescriptor;
	}

	private Method findGetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findGetterForProperty(propertyName, clazz, target instanceof Class);
		if (method == null && target instanceof Class) {
			method = findGetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	private Method findSetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findSetterForProperty(propertyName, clazz, target instanceof Class);
		if (method == null && target instanceof Class) {
			method = findSetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	private Field findField(String name, Class<?> clazz, Object target) {
		Field field = findField(name, clazz, target instanceof Class);
		if (field == null && target instanceof Class) {
			field = findField(name, target.getClass(), false);
		}
		return field;
	}

	/**
	 * Find a getter method for the specified property.
	 */
	protected Method findGetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				 "get", clazz, mustBeStatic, 0, ANY_TYPES);
		if (method == null) {
			method = findMethodForProperty(getPropertyMethodSuffixes(propertyName),
					 "is", clazz, mustBeStatic, 0, BOOLEAN_TYPES);
		}
		return method;
	}

	/**
	 * Find a setter method for the specified property.
	 */
	protected Method findSetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		return findMethodForProperty(getPropertyMethodSuffixes(propertyName),
				"set", clazz, mustBeStatic, 1, ANY_TYPES);
	}

	private Method findMethodForProperty(String[] methodSuffixes, String prefix, Class<?> clazz,
			boolean mustBeStatic, int numberOfParams, Set<Class<?>> requiredReturnTypes) {

		Method[] methods = getSortedClassMethods(clazz);
		for (String methodSuffix : methodSuffixes) {
			for (Method method : methods) {
				if (method.getName().equals(prefix + methodSuffix) &&
						method.getParameterTypes().length == numberOfParams &&
						(!mustBeStatic || Modifier.isStatic(method.getModifiers())) &&
						(requiredReturnTypes.isEmpty() || requiredReturnTypes.contains(method.getReturnType()))) {
					return method;
				}
			}
		}
		return null;

	}

	/**
	 * Returns class methods ordered with non bridge methods appearing higher.
	 */
	private Method[] getSortedClassMethods(Class<?> clazz) {
		Method[] methods = clazz.getMethods();
		Arrays.sort(methods, new Comparator<Method>() {
			@Override
			public int compare(Method o1, Method o2) {
				return (o1.isBridge() == o2.isBridge()) ? 0 : (o1.isBridge() ? 1 : -1);
			}
		});
		return methods;
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
			return new String[] { suffix };
		}
		return new String[] { suffix, StringUtils.capitalize(suffix) };
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

	/**
	 * Find a field of a certain name on a specified class.
	 */
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
	 * Attempt to create an optimized property accessor tailored for a property of a particular name on
	 * a particular class. The general ReflectivePropertyAccessor will always work but is not optimal
	 * due to the need to lookup which reflective member (method/field) to use each time read() is called.
	 * This method will just return the ReflectivePropertyAccessor instance if it is unable to build
	 * something more optimal.
	 */
	public PropertyAccessor createOptimalAccessor(EvaluationContext evalContext, Object target, String name) {
		// Don't be clever for arrays or null target
		if (target == null) {
			return this;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (type.isArray()) {
			return this;
		}

		CacheKey cacheKey = new CacheKey(type, name, target instanceof Class);
		InvokerPair invocationTarget = this.readerCache.get(cacheKey);

		if (invocationTarget == null || invocationTarget.member instanceof Method) {
			Method method = (Method) (invocationTarget==null?null:invocationTarget.member);
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					invocationTarget = new InvokerPair(method,new TypeDescriptor(new MethodParameter(method,-1)));
					ReflectionUtils.makeAccessible(method);
					this.readerCache.put(cacheKey, invocationTarget);
				}
			}
			if (method != null) {
				return new OptimalPropertyAccessor(invocationTarget);
			}
		}

		if (invocationTarget == null || invocationTarget.member instanceof Field) {
			Field field = (invocationTarget != null ? (Field) invocationTarget.member : null);
			if (field == null) {
				field = findField(name, type, target instanceof Class);
				if (field != null) {
					invocationTarget = new InvokerPair(field, new TypeDescriptor(field));
					ReflectionUtils.makeAccessible(field);
					this.readerCache.put(cacheKey, invocationTarget);
				}
			}
			if (field != null) {
				return new OptimalPropertyAccessor(invocationTarget);
			}
		}
		return this;
	}


	/**
	 * Captures the member (method/field) to call reflectively to access a property value
	 * and the type descriptor for the value returned by the reflective call.
	 */
	private static class InvokerPair {

		final Member member;

		final TypeDescriptor typeDescriptor;

		public InvokerPair(Member member, TypeDescriptor typeDescriptor) {
			this.member = member;
			this.typeDescriptor = typeDescriptor;
		}
	}


	private static class CacheKey {

		private final Class<?> clazz;

		private final String name;

		private boolean targetIsClass;

		public CacheKey(Class<?> clazz, String name, boolean targetIsClass) {
			this.clazz = clazz;
			this.name = name;
			this.targetIsClass = targetIsClass;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof CacheKey)) {
				return false;
			}
			CacheKey otherKey = (CacheKey) other;
			return (this.clazz.equals(otherKey.clazz) && this.name.equals(otherKey.name) &&
					this.targetIsClass == otherKey.targetIsClass);
		}

		@Override
		public int hashCode() {
			return (this.clazz.hashCode() * 29 + this.name.hashCode());
		}

		@Override
		public String toString() {
			return new ToStringCreator(this).append("clazz", this.clazz).append("name",
					this.name).append("targetIsClass", this.targetIsClass).toString();
		}
	}


	/**
	 * An optimized form of a PropertyAccessor that will use reflection but only knows
	 * how to access a particular property on a particular class. This is unlike the
	 * general ReflectivePropertyResolver which manages a cache of methods/fields that
	 * may be invoked to access different properties on different classes. This optimal
	 * accessor exists because looking up the appropriate reflective object by class/name
	 * on each read is not cheap.
	 */
	public static class OptimalPropertyAccessor implements CompilablePropertyAccessor {

		public final Member member;

		private final TypeDescriptor typeDescriptor;

		private final boolean needsToBeMadeAccessible;

		OptimalPropertyAccessor(InvokerPair target) {
			this.member = target.member;
			this.typeDescriptor = target.typeDescriptor;
			if (this.member instanceof Field) {
				Field field = (Field) this.member;
				this.needsToBeMadeAccessible = (!Modifier.isPublic(field.getModifiers()) ||
						!Modifier.isPublic(field.getDeclaringClass().getModifiers())) && !field.isAccessible();
			}
			else {
				Method method = (Method) this.member;
				this.needsToBeMadeAccessible = ((!Modifier.isPublic(method.getModifiers()) ||
						!Modifier.isPublic(method.getDeclaringClass().getModifiers())) && !method.isAccessible());
			}
		}

		@Override
		public Class<?>[] getSpecificTargetClasses() {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			if (target == null) {
				return false;
			}
			Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
			if (type.isArray()) {
				return false;
			}
			if (this.member instanceof Method) {
				Method method = (Method) this.member;
				String getterName = "get" + StringUtils.capitalize(name);
				if (getterName.equals(method.getName())) {
					return true;
				}
				getterName = "is" + StringUtils.capitalize(name);
				return getterName.equals(method.getName());
			}
			else {
				Field field = (Field) this.member;
				return field.getName().equals(name);
			}
		}

		@Override
		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			if (this.member instanceof Method) {
				try {
					if (this.needsToBeMadeAccessible) {
						ReflectionUtils.makeAccessible((Method) this.member);
					}
					Object value = ((Method) this.member).invoke(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter", ex);
				}
			}
			if (this.member instanceof Field) {
				try {
					if (this.needsToBeMadeAccessible) {
						ReflectionUtils.makeAccessible((Field) this.member);
					}
					Object value = ((Field) this.member).get(target);
					return new TypedValue(value, this.typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field: " + name, ex);
				}
			}
			throw new AccessException("Neither getter nor field found for property '" + name + "'");
		}

		@Override
		public boolean canWrite(EvaluationContext context, Object target, String name) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public void write(EvaluationContext context, Object target, String name, Object newValue) {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}

		@Override
		public boolean isCompilable() {
			return (Modifier.isPublic(this.member.getModifiers()) &&
					Modifier.isPublic(this.member.getDeclaringClass().getModifiers()));
		}

		@Override
		public Class<?> getPropertyType() {
			if (this.member instanceof Field) {
				return ((Field) this.member).getType();
			}
			else {
				return ((Method) this.member).getReturnType();
			}
		}

		@Override
		public void generateCode(String propertyName, MethodVisitor mv, CodeFlow cf) {
			boolean isStatic = Modifier.isStatic(this.member.getModifiers());
			String descriptor = cf.lastDescriptor();
			String memberDeclaringClassSlashedDescriptor = this.member.getDeclaringClass().getName().replace('.', '/');
			if (!isStatic) {
				if (descriptor == null) {
					cf.loadTarget(mv);
				}
				if (descriptor == null || !memberDeclaringClassSlashedDescriptor.equals(descriptor.substring(1))) {
					mv.visitTypeInsn(CHECKCAST, memberDeclaringClassSlashedDescriptor);
				}
			} else {
				if (descriptor != null) {
					// A static field/method call will not consume what is on the stack,
					// it needs to be popped off.
					mv.visitInsn(POP);
				}
			}
			if (this.member instanceof Field) {
				mv.visitFieldInsn(isStatic ? GETSTATIC : GETFIELD, memberDeclaringClassSlashedDescriptor,
						this.member.getName(), CodeFlow.toJvmDescriptor(((Field) this.member).getType()));
			}
			else {
				mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, memberDeclaringClassSlashedDescriptor,
						this.member.getName(), CodeFlow.createSignatureDescriptor((Method) this.member),false);
			}
		}
	}

}
