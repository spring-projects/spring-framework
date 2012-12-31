/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans;

import java.awt.Image;

import java.beans.BeanDescriptor;
import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.springframework.beans.PropertyDescriptorUtils.*;

/**
 * Decorator for a standard {@link BeanInfo} object, e.g. as created by
 * {@link Introspector#getBeanInfo(Class)}, designed to discover and register static
 * and/or non-void returning setter methods. For example:
 * <pre>{@code
 * public class Bean {
 *     private Foo foo;
 *
 *     public Foo getFoo() {
 *         return this.foo;
 *     }
 *
 *     public Bean setFoo(Foo foo) {
 *         this.foo = foo;
 *         return this;
 *     }
 * }</pre>
 * The standard JavaBeans {@code Introspector} will discover the {@code getFoo} read
 * method, but will bypass the {@code #setFoo(Foo)} write method, because its non-void
 * returning signature does not comply with the JavaBeans specification.
 * {@code ExtendedBeanInfo}, on the other hand, will recognize and include it. This is
 * designed to allow APIs with "builder" or method-chaining style setter signatures to be
 * used within Spring {@code <beans>} XML. {@link #getPropertyDescriptors()} returns all
 * existing property descriptors from the wrapped {@code BeanInfo} as well any added for
 * non-void returning setters. Both standard ("non-indexed") and
 * <a href="http://docs.oracle.com/javase/tutorial/javabeans/writing/properties.html">
 * indexed properties</a> are fully supported.
 *
 * @author Chris Beams
 * @since 3.1
 * @see #ExtendedBeanInfo(BeanInfo)
 * @see CachedIntrospectionResults
 */
class ExtendedBeanInfo implements BeanInfo {

	private final BeanInfo delegate;

	private final Set<PropertyDescriptor> propertyDescriptors =
			new TreeSet<PropertyDescriptor>(new PropertyDescriptorComparator());


	/**
	 * Wrap the given {@link BeanInfo} instance; copy all its existing property descriptors
	 * locally, wrapping each in a custom {@link SimpleIndexedPropertyDescriptor indexed} or
	 * {@link SimpleNonIndexedPropertyDescriptor non-indexed} {@code PropertyDescriptor}
	 * variant that bypasses default JDK weak/soft reference management; then search
	 * through its method descriptors to find any non-void returning write methods and
	 * update or create the corresponding {@link PropertyDescriptor} for each one found.
	 * @param delegate the wrapped {@code BeanInfo}, which is never modified
	 * @throws IntrospectionException if any problems occur creating and adding new
	 * property descriptors
	 * @see #getPropertyDescriptors()
	 */
	public ExtendedBeanInfo(BeanInfo delegate) throws IntrospectionException {
		this.delegate = delegate;

		for (PropertyDescriptor pd : delegate.getPropertyDescriptors()) {
			this.propertyDescriptors.add(pd instanceof IndexedPropertyDescriptor ?
					new SimpleIndexedPropertyDescriptor((IndexedPropertyDescriptor) pd) :
					new SimpleNonIndexedPropertyDescriptor(pd));
		}

		for (Method method : findCandidateWriteMethods(delegate.getMethodDescriptors())) {
			handleCandidateWriteMethod(method);
		}
	}


	private List<Method> findCandidateWriteMethods(MethodDescriptor[] methodDescriptors) {
		List<Method> matches = new ArrayList<Method>();
		for (MethodDescriptor methodDescriptor : methodDescriptors) {
			Method method = methodDescriptor.getMethod();
			if (isCandidateWriteMethod(method)) {
				matches.add(method);
			}
		}
		// sort non-void returning write methods to guard against the ill effects of
		// non-deterministic sorting of methods returned from Class#getDeclaredMethods
		// under JDK 7. See http://bugs.sun.com/view_bug.do?bug_id=7023180
		Collections.sort(matches, new Comparator<Method>() {
			public int compare(Method m1, Method m2) {
				return m2.toString().compareTo(m1.toString());
			}
		});
		return matches;
	}

	public static boolean isCandidateWriteMethod(Method method) {
		String methodName = method.getName();
		Class<?>[] parameterTypes = method.getParameterTypes();
		int nParams = parameterTypes.length;
		if (methodName.length() > 3 && methodName.startsWith("set") &&
				Modifier.isPublic(method.getModifiers()) &&
				(
						!void.class.isAssignableFrom(method.getReturnType()) ||
						Modifier.isStatic(method.getModifiers())
				) &&
				(nParams == 1 || (nParams == 2 && parameterTypes[0].equals(int.class)))) {
			return true;
		}
		return false;
	}

	private void handleCandidateWriteMethod(Method method) throws IntrospectionException {
		int nParams = method.getParameterTypes().length;
		String propertyName = propertyNameFor(method);
		Class<?> propertyType = method.getParameterTypes()[nParams-1];
		PropertyDescriptor existingPD = findExistingPropertyDescriptor(propertyName, propertyType);
		if (nParams == 1) {
			if (existingPD == null) {
				this.propertyDescriptors.add(
						new SimpleNonIndexedPropertyDescriptor(propertyName, null, method));
			}
			else {
				existingPD.setWriteMethod(method);
			}
		}
		else if (nParams == 2) {
			if (existingPD == null) {
				this.propertyDescriptors.add(
						new SimpleIndexedPropertyDescriptor(
								propertyName, null, null, null, method));
			}
			else if (existingPD instanceof IndexedPropertyDescriptor) {
				((IndexedPropertyDescriptor)existingPD).setIndexedWriteMethod(method);
			}
			else {
				this.propertyDescriptors.remove(existingPD);
				this.propertyDescriptors.add(
						new SimpleIndexedPropertyDescriptor(
								propertyName, existingPD.getReadMethod(),
								existingPD.getWriteMethod(), null, method));
			}
		}
		else {
			throw new IllegalArgumentException(
					"write method must have exactly 1 or 2 parameters: " + method);
		}
	}

	private PropertyDescriptor findExistingPropertyDescriptor(
			String propertyName, Class<?> propertyType) {

		for (PropertyDescriptor pd : this.propertyDescriptors) {
			final Class<?> candidateType;
			final String candidateName = pd.getName();
			if (pd instanceof IndexedPropertyDescriptor) {
				IndexedPropertyDescriptor ipd = (IndexedPropertyDescriptor) pd;
				candidateType = ipd.getIndexedPropertyType();
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) ||
								candidateType.equals(propertyType.getComponentType()))) {
					return pd;
				}
			}
			else {
				candidateType = pd.getPropertyType();
				if (candidateName.equals(propertyName) &&
						(candidateType.equals(propertyType) ||
								propertyType.equals(candidateType.getComponentType()))) {
					return pd;
				}
			}
		}
		return null;
	}

	private String propertyNameFor(Method method) {
		return Introspector.decapitalize(
				method.getName().substring(3, method.getName().length()));
	}


	/**
	 * Return the set of {@link PropertyDescriptor}s from the wrapped {@link BeanInfo}
	 * object as well as {@code PropertyDescriptor}s for each non-void returning setter
	 * method found during construction.
	 * @see #ExtendedBeanInfo(BeanInfo)
	 */
	public PropertyDescriptor[] getPropertyDescriptors() {
		return this.propertyDescriptors.toArray(
				new PropertyDescriptor[this.propertyDescriptors.size()]);
	}

	public BeanInfo[] getAdditionalBeanInfo() {
		return delegate.getAdditionalBeanInfo();
	}

	public BeanDescriptor getBeanDescriptor() {
		return delegate.getBeanDescriptor();
	}

	public int getDefaultEventIndex() {
		return delegate.getDefaultEventIndex();
	}

	public int getDefaultPropertyIndex() {
		return delegate.getDefaultPropertyIndex();
	}

	public EventSetDescriptor[] getEventSetDescriptors() {
		return delegate.getEventSetDescriptors();
	}

	public Image getIcon(int iconKind) {
		return delegate.getIcon(iconKind);
	}

	public MethodDescriptor[] getMethodDescriptors() {
		return delegate.getMethodDescriptors();
	}
}


class SimpleNonIndexedPropertyDescriptor extends PropertyDescriptor {

	private Method readMethod;
	private Method writeMethod;
	private Class<?> propertyType;
	private Class<?> propertyEditorClass;


	public SimpleNonIndexedPropertyDescriptor(PropertyDescriptor original)
			throws IntrospectionException {

		this(original.getName(), original.getReadMethod(), original.getWriteMethod());
		copyNonMethodProperties(original, this);
	}

	public SimpleNonIndexedPropertyDescriptor(String propertyName,
			Method readMethod, Method writeMethod) throws IntrospectionException {

		super(propertyName, null, null);
		this.setReadMethod(readMethod);
		this.setWriteMethod(writeMethod);
		this.propertyType = findPropertyType(readMethod, writeMethod);
	}


	@Override
	public Method getReadMethod() {
		return this.readMethod;
	}

	@Override
	public void setReadMethod(Method readMethod) {
		this.readMethod = readMethod;
	}

	@Override
	public Method getWriteMethod() {
		return this.writeMethod;
	}

	@Override
	public void setWriteMethod(Method writeMethod) {
		this.writeMethod = writeMethod;
	}

	@Override
	public Class<?> getPropertyType() {
		if (this.propertyType == null) {
			try {
				this.propertyType = findPropertyType(this.readMethod, this.writeMethod);
			} catch (IntrospectionException ex) {
				// ignore, as does PropertyDescriptor#getPropertyType
			}
		}
		return this.propertyType;
	}

	@Override
	public Class<?> getPropertyEditorClass() {
		return this.propertyEditorClass;
	}

	@Override
	public void setPropertyEditorClass(Class<?> propertyEditorClass) {
		this.propertyEditorClass = propertyEditorClass;
	}


	@Override
	public boolean equals(Object obj) {
		return PropertyDescriptorUtils.equals(this, obj);
	}

	@Override
	public String toString() {
		return String.format("%s[name=%s, propertyType=%s, readMethod=%s, writeMethod=%s]",
				this.getClass().getSimpleName(), this.getName(), this.getPropertyType(),
				this.readMethod, this.writeMethod);
	}
}


class SimpleIndexedPropertyDescriptor extends IndexedPropertyDescriptor {

	private Method readMethod;
	private Method writeMethod;
	private Class<?> propertyType;
	private Class<?> propertyEditorClass;

	private Method indexedReadMethod;
	private Method indexedWriteMethod;
	private Class<?> indexedPropertyType;


	public SimpleIndexedPropertyDescriptor(IndexedPropertyDescriptor original)
			throws IntrospectionException {

		this(original.getName(), original.getReadMethod(), original.getWriteMethod(),
				original.getIndexedReadMethod(), original.getIndexedWriteMethod());
		copyNonMethodProperties(original, this);
	}

	public SimpleIndexedPropertyDescriptor(String propertyName,
				Method readMethod, Method writeMethod,
				Method indexedReadMethod, Method indexedWriteMethod)
			throws IntrospectionException {

		super(propertyName, null, null, null, null);
		this.setReadMethod(readMethod);
		this.setWriteMethod(writeMethod);
		this.propertyType = findPropertyType(readMethod, writeMethod);

		this.setIndexedReadMethod(indexedReadMethod);
		this.setIndexedWriteMethod(indexedWriteMethod);
		this.indexedPropertyType = findIndexedPropertyType(
				this.getName(), this.propertyType, indexedReadMethod, indexedWriteMethod);
	}


	@Override
	public Method getReadMethod() {
		return this.readMethod;
	}

	@Override
	public void setReadMethod(Method readMethod) {
		this.readMethod = readMethod;
	}

	@Override
	public Method getWriteMethod() {
		return this.writeMethod;
	}

	@Override
	public void setWriteMethod(Method writeMethod) {
		this.writeMethod = writeMethod;
	}

	@Override
	public Class<?> getPropertyType() {
		if (this.propertyType == null) {
			try {
				this.propertyType = findPropertyType(this.readMethod, this.writeMethod);
			} catch (IntrospectionException ex) {
				// ignore, as does IndexedPropertyDescriptor#getPropertyType
			}
		}
		return this.propertyType;
	}

	@Override
	public Method getIndexedReadMethod() {
		return this.indexedReadMethod;
	}

	@Override
	public void setIndexedReadMethod(Method indexedReadMethod) throws IntrospectionException {
		this.indexedReadMethod = indexedReadMethod;
	}

	@Override
	public Method getIndexedWriteMethod() {
		return this.indexedWriteMethod;
	}

	@Override
	public void setIndexedWriteMethod(Method indexedWriteMethod) throws IntrospectionException {
		this.indexedWriteMethod = indexedWriteMethod;
	}

	@Override
	public Class<?> getIndexedPropertyType() {
		if (this.indexedPropertyType == null) {
			try {
				this.indexedPropertyType = findIndexedPropertyType(
						this.getName(), this.getPropertyType(),
						this.indexedReadMethod, this.indexedWriteMethod);
			} catch (IntrospectionException ex) {
				// ignore, as does IndexedPropertyDescriptor#getIndexedPropertyType
			}
		}
		return this.indexedPropertyType;
	}

	@Override
	public Class<?> getPropertyEditorClass() {
		return this.propertyEditorClass;
	}

	@Override
	public void setPropertyEditorClass(Class<?> propertyEditorClass) {
		this.propertyEditorClass = propertyEditorClass;
	}


	/*
	 * @see java.beans.IndexedPropertyDescriptor#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj != null && obj instanceof IndexedPropertyDescriptor) {
			IndexedPropertyDescriptor other = (IndexedPropertyDescriptor) obj;
			if (!compareMethods(getIndexedReadMethod(), other.getIndexedReadMethod())) {
				return false;
			}

			if (!compareMethods(getIndexedWriteMethod(), other.getIndexedWriteMethod())) {
				return false;
			}

			if (getIndexedPropertyType() != other.getIndexedPropertyType()) {
				return false;
			}
			return PropertyDescriptorUtils.equals(this, obj);
		}
		return false;
	}

	@Override
	public String toString() {
		return String.format("%s[name=%s, propertyType=%s, indexedPropertyType=%s, " +
				"readMethod=%s, writeMethod=%s, indexedReadMethod=%s, indexedWriteMethod=%s]",
				this.getClass().getSimpleName(), this.getName(), this.getPropertyType(),
				this.getIndexedPropertyType(), this.readMethod, this.writeMethod,
				this.indexedReadMethod, this.indexedWriteMethod);
	}
}


class PropertyDescriptorUtils {

	/*
	 * see java.beans.FeatureDescriptor#FeatureDescriptor(FeatureDescriptor)
	 */
	public static void copyNonMethodProperties(PropertyDescriptor source, PropertyDescriptor target)
			throws IntrospectionException {

		target.setExpert(source.isExpert());
		target.setHidden(source.isHidden());
		target.setPreferred(source.isPreferred());
		target.setName(source.getName());
		target.setShortDescription(source.getShortDescription());
		target.setDisplayName(source.getDisplayName());

		// copy all attributes (emulating behavior of private FeatureDescriptor#addTable)
		Enumeration<String> keys = source.attributeNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			target.setValue(key, source.getValue(key));
		}

		// see java.beans.PropertyDescriptor#PropertyDescriptor(PropertyDescriptor)
		target.setPropertyEditorClass(source.getPropertyEditorClass());
		target.setBound(source.isBound());
		target.setConstrained(source.isConstrained());
	}

	/*
	 * See PropertyDescriptor#findPropertyType
	 */
	public static Class<?> findPropertyType(Method readMethod, Method writeMethod)
			throws IntrospectionException {

		Class<?> propertyType = null;

		if (readMethod != null) {
			Class<?>[] params = readMethod.getParameterTypes();
			if (params.length != 0) {
				throw new IntrospectionException("bad read method arg count: " + readMethod);
			}
			propertyType = readMethod.getReturnType();
			if (propertyType == Void.TYPE) {
				throw new IntrospectionException("read method "
						+ readMethod.getName() + " returns void");
			}
		}
		if (writeMethod != null) {
			Class<?> params[] = writeMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException("bad write method arg count: " + writeMethod);
			}
			if (propertyType != null
					&& !params[0].isAssignableFrom(propertyType)) {
				throw new IntrospectionException("type mismatch between read and write methods");
			}
			propertyType = params[0];
		}
		return propertyType;
	}

	/*
	 * See IndexedPropertyDescriptor#findIndexedPropertyType
	 */
	public static Class<?> findIndexedPropertyType(String name, Class<?> propertyType,
				Method indexedReadMethod, Method indexedWriteMethod)
			throws IntrospectionException {

		Class<?> indexedPropertyType = null;

		if (indexedReadMethod != null) {
			Class<?> params[] = indexedReadMethod.getParameterTypes();
			if (params.length != 1) {
				throw new IntrospectionException(
						"bad indexed read method arg count");
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException(
						"non int index to indexed read method");
			}
			indexedPropertyType = indexedReadMethod.getReturnType();
			if (indexedPropertyType == Void.TYPE) {
				throw new IntrospectionException(
						"indexed read method returns void");
			}
		}
		if (indexedWriteMethod != null) {
			Class<?> params[] = indexedWriteMethod.getParameterTypes();
			if (params.length != 2) {
				throw new IntrospectionException(
						"bad indexed write method arg count");
			}
			if (params[0] != Integer.TYPE) {
				throw new IntrospectionException(
						"non int index to indexed write method");
			}
			if (indexedPropertyType != null && indexedPropertyType != params[1]) {
				throw new IntrospectionException(
						"type mismatch between indexed read and indexed write methods: " + name);
			}
			indexedPropertyType = params[1];
		}
		if (propertyType != null
				&& (!propertyType.isArray() ||
						propertyType.getComponentType() != indexedPropertyType)) {
			throw new IntrospectionException(
					"type mismatch between indexed and non-indexed methods: " + name);
		}
		return indexedPropertyType;
	}

	/**
	 * Compare the given {@link PropertyDescriptor} against the given {@link Object} and
	 * return {@code true} if they are objects are equivalent, i.e. both are {@code
	 * PropertyDescriptor}s whose read method, write method, property types, property
	 * editor and flags are equivalent.
	 *
	 * @see PropertyDescriptor#equals(Object)
	 */
	public static boolean equals(PropertyDescriptor pd1, Object obj) {
		if (pd1 == obj) {
			return true;
		}
		if (obj != null && obj instanceof PropertyDescriptor) {
			PropertyDescriptor pd2 = (PropertyDescriptor) obj;
			if (!compareMethods(pd1.getReadMethod(), pd2.getReadMethod())) {
				return false;
			}

			if (!compareMethods(pd1.getWriteMethod(), pd2.getWriteMethod())) {
				return false;
			}

			if (pd1.getPropertyType() == pd2.getPropertyType()
					&& pd1.getPropertyEditorClass() == pd2.getPropertyEditorClass()
					&& pd1.isBound() == pd2.isBound()
					&& pd1.isConstrained() == pd2.isConstrained()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * see PropertyDescriptor#compareMethods
	 */
	public static boolean compareMethods(Method a, Method b) {
		if ((a == null) != (b == null)) {
			return false;
		}

		if (a != null && b != null) {
			if (!a.equals(b)) {
				return false;
			}
		}
		return true;
	}
}


/**
 * Sorts PropertyDescriptor instances alpha-numerically to emulate the behavior of
 * {@link java.beans.BeanInfo#getPropertyDescriptors()}.
 *
 * @see ExtendedBeanInfo#propertyDescriptors
 */
class PropertyDescriptorComparator implements Comparator<PropertyDescriptor> {

	public int compare(PropertyDescriptor desc1, PropertyDescriptor desc2) {
		String left = desc1.getName();
		String right = desc2.getName();
		for (int i = 0; i < left.length(); i++) {
			if (right.length() == i) {
				return 1;
			}
			int result = left.getBytes()[i] - right.getBytes()[i];
			if (result != 0) {
				return result;
			}
		}
		return left.length() - right.length();
	}
}
