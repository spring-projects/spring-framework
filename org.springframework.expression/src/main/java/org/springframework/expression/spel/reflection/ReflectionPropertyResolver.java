/*
 * Copyright 2004-2008 the original author or authors.
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
package org.springframework.expression.spel.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.expression.CacheablePropertyAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyReaderExecutor;
import org.springframework.expression.PropertyWriterExecutor;

/**
 * Simple PropertyResolver that uses reflection to access properties for reading and writing. A property can be accessed
 * if it is accessible as a field on the object or through a getter (if being read) or a setter (if being written). This
 * implementation currently follows the Resolver/Executor model (it extends CacheablePropertyAccessor) - the code that
 * would be used if it were a simple property accessor is shown at the end.
 * 
 * @author Andy Clement
 */
public class ReflectionPropertyResolver extends CacheablePropertyAccessor {

	/**
	 * @return null which means this is a general purpose accessor
	 */
	public Class<?>[] getSpecificTargetClasses() {
		return null;
	}

	/**
	 * Use reflection to discover if a named property is accessible on an target type and if it is return an executor
	 * object that can be called repeatedly to retrieve that property. A property is accessible either as a field or
	 * through a getter.
	 * 
	 * @param context the context in which the access is being attempted
	 * @param target the target object on which the property is being accessed
	 * @param name the name of the property
	 */
	@Override
	public PropertyReaderExecutor getReaderAccessor(EvaluationContext context, Object target, Object name) {
		if (target == null) {
			return null;
		}
		Class<?> relevantClass = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (!(name instanceof String)) {
			// A property not found exception will occur if the reflection finder was supposed to find it
			return null;
		}
		String propertyName = (String) name;
		if (relevantClass.isArray() && propertyName.equals("length")) {
			return new ReflectionPropertyReaderExecutorForArrayLength();
		}
		Field field = ReflectionUtils.findField(propertyName, relevantClass);
		if (field != null) {
			return new ReflectionPropertyReaderExecutor(propertyName, field);
		}
		Method m = ReflectionUtils.findGetterForProperty(propertyName, relevantClass);
		if (m != null) {
			return new ReflectionPropertyReaderExecutor(propertyName, m);
		}
		return null;
	}

	/**
	 * Use reflection to discover if a named property is accessible on an target type and if it is return an executor
	 * object that can be called repeatedly to set that property. A property is writable either as a field or through a
	 * setter.
	 * 
	 * @param context the context in which the set is being attempted
	 * @param target the target object on which the property is being set
	 * @param name the name of the property
	 */
	@Override
	public PropertyWriterExecutor getWriterAccessor(EvaluationContext context, Object target, Object name) {
		if (target == null) {
			return null;
		}
		Class<?> relevantClass = (target instanceof Class ? (Class<?>) target : target.getClass());
		if (!(name instanceof String)) {
			// A property not found exception will occur if the reflection finder was supposed to find it
			return null;
		}
		Field field = ReflectionUtils.findField((String) name, relevantClass);
		if (field != null) {
			return new ReflectionPropertyWriterExecutor((String) name, field);
		}
		Method m = ReflectionUtils.findSetterForProperty((String) name, relevantClass);
		if (m != null) {
			return new ReflectionPropertyWriterExecutor((String) name, m);
		}
		return null;
	}

	// /**
	// * Return true if the resolver is able to read the specified property from the specified target.
	// */
	// public boolean canRead(EvaluationContext relatedContext, Object target, Object name) throws AccessException {
	// if (target==null) {
	// return false;
	// }
	// Class<?> relevantClass = (target instanceof Class ? (Class<?>) target : target.getClass());
	// if (!(name instanceof String)) {
	// return false;
	// }
	// String propertyName = (String) name;
	// Field field = ReflectionUtils.findField(propertyName, relevantClass);
	// if (field != null) {
	// return true;
	// }
	// Method m = ReflectionUtils.findGetterForProperty(propertyName, relevantClass);
	// if (m != null) {
	// return true;
	// }
	// return false;
	// }
	//
	// /**
	// * Read the specified property from the specified target. //
	// */
	// public Object read(EvaluationContext context, Object target, Object name) throws AccessException {
	// if (target==null) {
	// return null;
	// }
	// Class<?> relevantClass = (target instanceof Class ? (Class<?>) target : target.getClass());
	// if (!(name instanceof String)) {
	// return null;
	// }
	// String propertyName = (String) name;
	// Field field = ReflectionUtils.findField(propertyName, relevantClass);
	// if (field != null) {
	// try {
	// if (!field.isAccessible()) {
	// field.setAccessible(true);
	// }
	// return field.get(target);
	// } catch (IllegalArgumentException e) {
	// throw new AccessException("Unable to access field: " + name, e);
	// } catch (IllegalAccessException e) {
	// throw new AccessException("Unable to access field: " + name, e);
	// }
	// }
	// Method m = ReflectionUtils.findGetterForProperty(propertyName, relevantClass);
	// if (m != null) {
	// try {
	// if (!m.isAccessible())
	// m.setAccessible(true);
	// return m.invoke(target);
	// } catch (IllegalArgumentException e) {
	// throw new AccessException("Unable to access property '" + name + "' through getter", e);
	// } catch (IllegalAccessException e) {
	// throw new AccessException("Unable to access property '" + name + "' through getter", e);
	// } catch (InvocationTargetException e) {
	// throw new AccessException("Unable to access property '" + name + "' through getter", e);
	// }
	// }
	// return null;
	// }
	// public void write(EvaluationContext context, Object target, Object name, Object newValue) throws AccessException
	// {
	// if (target==null) {
	// return;
	// }
	// Class<?> relevantClass = (target instanceof Class ? (Class<?>) target : target.getClass());
	// if (!(name instanceof String))
	// return;
	// Field field = ReflectionUtils.findField((String) name, relevantClass);
	// if (field != null) {
	// try {
	// if (!field.isAccessible())
	// field.setAccessible(true);
	// field.set(target, newValue);
	// } catch (IllegalArgumentException e) {
	// throw new AccessException("Unable to write to property '" + name + "'", e);
	// } catch (IllegalAccessException e) {
	// throw new AccessException("Unable to write to property '" + name + "'", e);
	// }
	// }
	// Method m = ReflectionUtils.findSetterForProperty((String) name, relevantClass);
	// if (m != null) {
	// try {
	// if (!m.isAccessible())
	// m.setAccessible(true);
	// m.invoke(target, newValue);
	// } catch (IllegalArgumentException e) {
	// throw new AccessException("Unable to access property '" + name + "' through setter", e);
	// } catch (IllegalAccessException e) {
	// throw new AccessException("Unable to access property '" + name + "' through setter", e);
	// } catch (InvocationTargetException e) {
	// throw new AccessException("Unable to access property '" + name + "' through setter", e);
	// }
	// }
	// }
	//
	//
	// public boolean canWrite(EvaluationContext context, Object target, Object name) throws AccessException {
	// if (target==null) {
	// return false;
	// }
	// Class<?> relevantClass = (target instanceof Class ? (Class<?>) target : target.getClass());
	// if (!(name instanceof String))
	// return false;
	// Field field = ReflectionUtils.findField((String) name, relevantClass);
	// if (field != null)
	// return true;
	// Method m = ReflectionUtils.findSetterForProperty((String) name, relevantClass);
	// if (m != null)
	// return true;
	// return false;
	// }
}
