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

package org.springframework.expression.spel.support;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.Property;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple PropertyAccessor that uses reflection to access properties for reading and writing. A property can be accessed
 * if it is accessible as a field on the object or through a getter (if being read) or a setter (if being written). 
 * 
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectivePropertyAccessor implements PropertyAccessor {

	protected final Map<CacheKey, InvokerPair> readerCache = new ConcurrentHashMap<CacheKey, InvokerPair>();

	protected final Map<CacheKey, Member> writerCache = new ConcurrentHashMap<CacheKey, Member>();
	
	protected final Map<CacheKey, TypeDescriptor> typeDescriptorCache = new ConcurrentHashMap<CacheKey, TypeDescriptor>();


	/**
	 * @return null which means this is a general purpose accessor
	 */
	public Class<?>[] getSpecificTargetClasses() {
		return null;
	}

	public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
		if (target == null) {
			return false;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (type.isArray() && name.equals("length")) {
			return true;
		}
		CacheKey cacheKey = new CacheKey(type, name);
		if (this.readerCache.containsKey(cacheKey)) {
			return true;
		}
		Method method = findGetterForProperty(name, type, target);
		if (method != null) {
			// Treat it like a property
			// The readerCache will only contain gettable properties (let's not worry about setters for now)
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
				this.readerCache.put(cacheKey, new InvokerPair(field,typeDescriptor));
				this.typeDescriptorCache.put(cacheKey, typeDescriptor);
				return true;
			}
		}
		return false;
	}

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

		CacheKey cacheKey = new CacheKey(type, name);
		InvokerPair invoker = this.readerCache.get(cacheKey);

		if (invoker == null || invoker.member instanceof Method) {
			Method method = (Method) (invoker != null ? invoker.member : null);
			if (method == null) {
				method = findGetterForProperty(name, type, target);
				if (method != null) {
					// TODO remove the duplication here between canRead and read
					// Treat it like a property
					// The readerCache will only contain gettable properties (let's not worry about setters for now)
					Property property = new Property(type, method, null);
					TypeDescriptor typeDescriptor = new TypeDescriptor(property);
					invoker = new InvokerPair(method, typeDescriptor);
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

	public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
		if (target == null) {
			return false;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		CacheKey cacheKey = new CacheKey(type, name);
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
		CacheKey cacheKey = new CacheKey(type, name);
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
		CacheKey cacheKey = new CacheKey(type, name);
		TypeDescriptor typeDescriptor =  this.typeDescriptorCache.get(cacheKey);
		if (typeDescriptor == null) {
			// attempt to populate the cache entry
			try {
				if (canRead(context, target, name)) {
					typeDescriptor =  this.typeDescriptorCache.get(cacheKey);
				}
				else if (canWrite(context, target, name)) {
					typeDescriptor =  this.typeDescriptorCache.get(cacheKey);
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
		if(method == null && target instanceof Class) {
			method = findGetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	private Method findSetterForProperty(String propertyName, Class<?> clazz, Object target) {
		Method method = findSetterForProperty(propertyName, clazz, target instanceof Class);
		if(method == null && target instanceof Class) {
			method = findSetterForProperty(propertyName, target.getClass(), false);
		}
		return method;
	}

	private Field findField(String name, Class<?> clazz, Object target) {
		Field field = findField(name, clazz, target instanceof Class);
		if(field == null && target instanceof Class) {
			field = findField(name, target.getClass(), false);
		}
		return field;
	}

	/**
	 * Find a getter method for the specified property. A getter is defined as a method whose name start with the prefix
	 * 'get' and the rest of the name is the same as the property name (with the first character uppercased).
	 */
	protected Method findGetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method[] ms = clazz.getMethods();
		// Try "get*" method...
		String getterName = "get" + StringUtils.capitalize(propertyName);
		for (Method method : ms) {
			if (method.getName().equals(getterName) && method.getParameterTypes().length == 0 &&
					(!mustBeStatic || Modifier.isStatic(method.getModifiers()))) {
				return method;
			}
		}
		// Try "is*" method...
		getterName = "is" + StringUtils.capitalize(propertyName);
		for (Method method : ms) {
			if (method.getName().equals(getterName) && method.getParameterTypes().length == 0 &&
					boolean.class.equals(method.getReturnType()) &&
					(!mustBeStatic || Modifier.isStatic(method.getModifiers()))) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Find a setter method for the specified property.
	 */
	protected Method findSetterForProperty(String propertyName, Class<?> clazz, boolean mustBeStatic) {
		Method[] methods = clazz.getMethods();
		String setterName = "set" + StringUtils.capitalize(propertyName);
		for (Method method : methods) {
			if (method.getName().equals(setterName) && method.getParameterTypes().length == 1 &&
					(!mustBeStatic || Modifier.isStatic(method.getModifiers()))) {
				return method;
			}
		}
		return null;
	}

	/**
	 * Find a field of a certain name on a specified class
	 */
	protected Field findField(String name, Class<?> clazz, boolean mustBeStatic) {
		Field[] fields = clazz.getFields();
		for (Field field : fields) {
			if (field.getName().equals(name) && (!mustBeStatic || Modifier.isStatic(field.getModifiers()))) {
				return field;
			}
		}
		return null;
	}
	
	/**
	 * Captures the member (method/field) to call reflectively to access a property value and the type descriptor for the
	 * value returned by the reflective call.
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

		private final Class clazz;

		private final String name;

		public CacheKey(Class clazz, String name) {
			this.clazz = clazz;
			this.name = name;
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
			return (this.clazz.equals(otherKey.clazz) && this.name.equals(otherKey.name));
		}

		@Override
		public int hashCode() {
			return this.clazz.hashCode() * 29 + this.name.hashCode();
		}
	}

	/** 
	 * Attempt to create an optimized property accessor tailored for a property of a particular name on
	 * a particular class. The general ReflectivePropertyAccessor will always work but is not optimal 
	 * due to the need to lookup which reflective member (method/field) to use each time read() is called.
	 * This method will just return the ReflectivePropertyAccessor instance if it is unable to build
	 * something more optimal.
	 */
	public PropertyAccessor createOptimalAccessor(EvaluationContext eContext, Object target, String name) {
		// Don't be clever for arrays or null target
		if (target == null) {
			return this;
		}
		Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (type.isArray()) {
			return this;
		}

		CacheKey cacheKey = new CacheKey(type, name);
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
			Field field = (Field) (invocationTarget==null?null:invocationTarget.member);
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
	 * An optimized form of a PropertyAccessor that will use reflection but only knows how to access a particular property 
	 * on a particular class.  This is unlike the general ReflectivePropertyResolver which manages a cache of methods/fields that 
	 * may be invoked to access different properties on different classes.  This optimal accessor exists because looking up
	 * the appropriate reflective object by class/name on each read is not cheap.
	 */
	static class OptimalPropertyAccessor implements PropertyAccessor {
		private final Member member;
		private final TypeDescriptor typeDescriptor;
		private final boolean needsToBeMadeAccessible;
		
		OptimalPropertyAccessor(InvokerPair target) {
			this.member = target.member;			
			this.typeDescriptor = target.typeDescriptor;
			if (this.member instanceof Field) {
				Field field = (Field)member;
				needsToBeMadeAccessible = (!Modifier.isPublic(field.getModifiers()) || !Modifier.isPublic(field.getDeclaringClass().getModifiers())) 
				&& !field.isAccessible();
			}
			else {
				Method method = (Method)member;
				needsToBeMadeAccessible = ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass().getModifiers()))
						&& !method.isAccessible());
			}
		}

		public Class[] getSpecificTargetClasses() {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}
		
		public boolean canRead(EvaluationContext context, Object target, String name) throws AccessException {
			if (target == null) {
				return false;
			}
			Class<?> type = (target instanceof Class ? (Class<?>) target : target.getClass());
			if (type.isArray()) {
				return false;
			}
			if (member instanceof Method) {
				Method method = (Method)member;
				String getterName = "get" + StringUtils.capitalize(name);
				if (getterName.equals(method.getName())) {
					return true;
				}
				getterName = "is" + StringUtils.capitalize(name);
				return getterName.equals(method.getName());
			}
			else {
				Field field = (Field)member;
				return field.getName().equals(name);
			}
		}

		public TypedValue read(EvaluationContext context, Object target, String name) throws AccessException {
			if (member instanceof Method) {
				try {
					if (needsToBeMadeAccessible) {
						ReflectionUtils.makeAccessible((Method) member);
					}
					Object value = ((Method) member).invoke(target);
					return new TypedValue(value, typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter", ex);
				}
			}		
			if (member instanceof Field) {
				try {
					if (needsToBeMadeAccessible) {
						ReflectionUtils.makeAccessible((Field)member);
					}
					Object value = ((Field)member).get(target);
					return new TypedValue(value, typeDescriptor.narrow(value));
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access field: " + name, ex);
				}
			}
			throw new AccessException("Neither getter nor field found for property '" + name + "'");
		}

		public boolean canWrite(EvaluationContext context, Object target, String name) throws AccessException {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}
		
		public void write(EvaluationContext context, Object target, String name, Object newValue)
				throws AccessException {
			throw new UnsupportedOperationException("Should not be called on an OptimalPropertyAccessor");
		}
	}

}
