/*
 * Copyright 2002-2009 the original author or authors.
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
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.AccessException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypedValue;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

/**
 * Simple PropertyResolver that uses reflection to access properties for reading and writing. A property can be accessed
 * if it is accessible as a field on the object or through a getter (if being read) or a setter (if being written). 
 * 
 * @author Andy Clement
 * @author Juergen Hoeller
 * @since 3.0
 */
public class ReflectivePropertyResolver implements PropertyAccessor {

	protected final Map<CacheKey, Member> readerCache = new ConcurrentHashMap<CacheKey, Member>();

	protected final Map<CacheKey, Member> writerCache = new ConcurrentHashMap<CacheKey, Member>();
	
	protected final Map<CacheKey, TypeDescriptor> typeDescriptorCache = new ConcurrentHashMap<CacheKey,TypeDescriptor>();

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
		if ((type.isArray() && name.equals("length"))) {
			return true;
		}
		CacheKey cacheKey = new CacheKey(type, name);
		if (this.readerCache.containsKey(cacheKey)) {
			return true;
		}
		Method method = findGetterForProperty(name, type, target instanceof Class);
		if (method != null) {
			this.readerCache.put(cacheKey, method);
			this.typeDescriptorCache.put(cacheKey, new TypeDescriptor(new MethodParameter(method,-1)));
			return true;
		}
		else {
			Field field = findField(name, type, target instanceof Class);
			if (field != null) {
				this.readerCache.put(cacheKey, field);	
				this.typeDescriptorCache.put(cacheKey, new TypeDescriptor(field));
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
			return new TypedValue(Array.getLength(target),TypeDescriptor.valueOf(Integer.TYPE));
		}

		CacheKey cacheKey = new CacheKey(type, name);
		Member cachedMember = this.readerCache.get(cacheKey);

		if (cachedMember == null || cachedMember instanceof Method) {
			Method method = (Method) cachedMember;
			if (method == null) {
				method = findGetterForProperty(name, type, target instanceof Class);
				if (method != null) {
					cachedMember = method;
					this.readerCache.put(cacheKey, cachedMember);
				}
			}
			if (method != null) {
				try {
					ReflectionUtils.makeAccessible(method);
					TypeDescriptor resultTypeDescriptor = new TypeDescriptor(new MethodParameter(method,-1));
					return new TypedValue(method.invoke(target),resultTypeDescriptor);
				}
				catch (Exception ex) {
					throw new AccessException("Unable to access property '" + name + "' through getter", ex);
				}
			}
		}

		if (cachedMember == null || cachedMember instanceof Field) {
			Field field = (Field) cachedMember;
			if (field == null) {
				field = findField(name, type, target instanceof Class);
				if (field != null) {
					cachedMember = field;
					this.readerCache.put(cacheKey, cachedMember);
				}
			}
			if (field != null) {
				try {
					ReflectionUtils.makeAccessible(field);
					return new TypedValue(field.get(target),new TypeDescriptor(field));
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
		Method method = findSetterForProperty(name, type, target instanceof Class);
		if (method != null) {
			this.writerCache.put(cacheKey, method);
			this.typeDescriptorCache.put(cacheKey, new TypeDescriptor(new MethodParameter(method,0)));
			return true;
		}
		else {
			Field field = findField(name, type, target instanceof Class);
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
				possiblyConvertedNewValue = context.getTypeConverter().convertValue(newValue, typeDescriptor);
			} catch (EvaluationException evaluationException) {
				throw new AccessException("Type conversion failure",evaluationException);
			}
		}
		
		CacheKey cacheKey = new CacheKey(type, name);
		Member cachedMember = this.writerCache.get(cacheKey);

		if (cachedMember == null || cachedMember instanceof Method) {
			Method method = (Method) cachedMember;
			if (method == null) {
				method = findSetterForProperty(name, type, target instanceof Class);
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
				field = findField(name, type, target instanceof Class);
				if (field != null) {
					cachedMember = field;
					this.readerCache.put(cacheKey, cachedMember);
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
				} else if (canWrite(context, target, name)) {
					typeDescriptor =  this.typeDescriptorCache.get(cacheKey);
				}
			} catch (AccessException e) {
				// continue with null typeDescriptor
			}
		}
		return typeDescriptor;
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

}
