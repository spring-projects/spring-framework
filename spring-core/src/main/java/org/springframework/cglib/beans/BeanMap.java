/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.beans;

import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.asm.ClassVisitor;
import org.springframework.cglib.core.AbstractClassGenerator;
import org.springframework.cglib.core.KeyFactory;
import org.springframework.cglib.core.ReflectUtils;

/**
 * A <code>Map</code>-based view of a JavaBean.  The default set of keys is the
 * union of all property names (getters or setters). An attempt to set
 * a read-only property will be ignored, and write-only properties will
 * be returned as <code>null</code>. Removal of objects is not a
 * supported (the key set is fixed).
 * @author Chris Nokleberg
 */
@SuppressWarnings({"rawtypes", "unchecked"})
abstract public class BeanMap implements Map {
	/**
	 * Limit the properties reflected in the key set of the map
	 * to readable properties.
	 * @see BeanMap.Generator#setRequire
	 */
	public static final int REQUIRE_GETTER = 1;

	/**
	 * Limit the properties reflected in the key set of the map
	 * to writable properties.
	 * @see BeanMap.Generator#setRequire
	 */
	public static final int REQUIRE_SETTER = 2;

	/**
	 * Helper method to create a new <code>BeanMap</code>.  For finer
	 * control over the generated instance, use a new instance of
	 * <code>BeanMap.Generator</code> instead of this static method.
	 * @param bean the JavaBean underlying the map
	 * @return a new <code>BeanMap</code> instance
	 */
	public static BeanMap create(Object bean) {
		Generator gen = new Generator();
		gen.setBean(bean);
		return gen.create();
	}

	public static class Generator extends AbstractClassGenerator {
		private static final Source SOURCE = new Source(BeanMap.class.getName());

		private static final BeanMapKey KEY_FACTORY =
		  (BeanMapKey)KeyFactory.create(BeanMapKey.class, KeyFactory.CLASS_BY_NAME);

		interface BeanMapKey {
			public Object newInstance(Class type, int require);
		}

		private Object bean;
		private Class beanClass;
		private int require;

		public Generator() {
			super(SOURCE);
		}

		/**
		 * Set the bean that the generated map should reflect. The bean may be swapped
		 * out for another bean of the same type using {@link #setBean}.
		 * Calling this method overrides any value previously set using {@link #setBeanClass}.
		 * You must call either this method or {@link #setBeanClass} before {@link #create}.
		 * @param bean the initial bean
		 */
		public void setBean(Object bean) {
			this.bean = bean;
			if (bean != null) {
				beanClass = bean.getClass();
				// SPRING PATCH BEGIN
				setContextClass(beanClass);
				// SPRING PATCH END
			}
		}

		/**
		 * Set the class of the bean that the generated map should support.
		 * You must call either this method or {@link #setBeanClass} before {@link #create}.
		 * @param beanClass the class of the bean
		 */
		public void setBeanClass(Class beanClass) {
			this.beanClass = beanClass;
		}

		/**
		 * Limit the properties reflected by the generated map.
		 * @param require any combination of {@link #REQUIRE_GETTER} and
		 * {@link #REQUIRE_SETTER}; default is zero (any property allowed)
		 */
		public void setRequire(int require) {
			this.require = require;
		}

		@Override
		protected ClassLoader getDefaultClassLoader() {
			return beanClass.getClassLoader();
		}

		@Override
		protected ProtectionDomain getProtectionDomain() {
			return ReflectUtils.getProtectionDomain(beanClass);
		}

		/**
		 * Create a new instance of the <code>BeanMap</code>. An existing
		 * generated class will be reused if possible.
		 */
		public BeanMap create() {
			if (beanClass == null) {
				throw new IllegalArgumentException("Class of bean unknown");
			}
			setNamePrefix(beanClass.getName());
			return (BeanMap)super.create(KEY_FACTORY.newInstance(beanClass, require));
		}

		@Override
		public void generateClass(ClassVisitor v) throws Exception {
			new BeanMapEmitter(v, getClassName(), beanClass, require);
		}

		@Override
		protected Object firstInstance(Class type) {
			return ((BeanMap)ReflectUtils.newInstance(type)).newInstance(bean);
		}

		@Override
		protected Object nextInstance(Object instance) {
			return ((BeanMap)instance).newInstance(bean);
		}
	}

	/**
	 * Create a new <code>BeanMap</code> instance using the specified bean.
	 * This is faster than using the {@link #create} static method.
	 * @param bean the JavaBean underlying the map
	 * @return a new <code>BeanMap</code> instance
	 */
	abstract public BeanMap newInstance(Object bean);

	/**
	 * Get the type of a property.
	 * @param name the name of the JavaBean property
	 * @return the type of the property, or null if the property does not exist
	 */
	abstract public Class getPropertyType(String name);

	protected Object bean;

	protected BeanMap() {
	}

	protected BeanMap(Object bean) {
		setBean(bean);
	}

	@Override
	public Object get(Object key) {
		return get(bean, key);
	}

	@Override
	public Object put(Object key, Object value) {
		return put(bean, key, value);
	}

	/**
	 * Get the property of a bean. This allows a <code>BeanMap</code>
	 * to be used statically for multiple beans--the bean instance tied to the
	 * map is ignored and the bean passed to this method is used instead.
	 * @param bean the bean to query; must be compatible with the type of
	 * this <code>BeanMap</code>
	 * @param key must be a String
	 * @return the current value, or null if there is no matching property
	 */
	abstract public Object get(Object bean, Object key);

	/**
	 * Set the property of a bean. This allows a <code>BeanMap</code>
	 * to be used statically for multiple beans--the bean instance tied to the
	 * map is ignored and the bean passed to this method is used instead.
	 * @param key must be a String
	 * @return the old value, if there was one, or null
	 */
	abstract public Object put(Object bean, Object key, Object value);

	/**
	 * Change the underlying bean this map should use.
	 * @param bean the new JavaBean
	 * @see #getBean
	 */
	public void setBean(Object bean) {
		this.bean = bean;
	}

	/**
	 * Return the bean currently in use by this map.
	 * @return the current JavaBean
	 * @see #setBean
	 */
	public Object getBean() {
		return bean;
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean containsKey(Object key) {
		return keySet().contains(key);
	}

	@Override
	public boolean containsValue(Object value) {
		for (Iterator it = keySet().iterator(); it.hasNext();) {
			Object v = get(it.next());
			if (((value == null) && (v == null)) || (value != null && value.equals(v))) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int size() {
		return keySet().size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0;
	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map t) {
		for (Object key : t.keySet()) {
			put(key, t.get(key));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || !(o instanceof Map)) {
			return false;
		}
		Map other = (Map)o;
		if (size() != other.size()) {
			return false;
		}
		for (Object key : keySet()) {
			if (!other.containsKey(key)) {
				return false;
			}
			Object v1 = get(key);
			Object v2 = other.get(key);
			if (!((v1 == null) ? v2 == null : v1.equals(v2))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int code = 0;
		for (Object key : keySet()) {
			Object value = get(key);
			code += ((key == null) ? 0 : key.hashCode()) ^
				((value == null) ? 0 : value.hashCode());
		}
		return code;
	}

	// TODO: optimize
	@Override
	public Set entrySet() {
		HashMap copy = new HashMap();
		for (Object key : keySet()) {
			copy.put(key, get(key));
		}
		return Collections.unmodifiableMap(copy).entrySet();
	}

	@Override
	public Collection values() {
		Set keys = keySet();
		List values = new ArrayList(keys.size());
		for (Iterator it = keys.iterator(); it.hasNext();) {
			values.add(get(it.next()));
		}
		return Collections.unmodifiableCollection(values);
	}

	/*
	 * @see java.util.AbstractMap#toString
	 */
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		for (Iterator it = keySet().iterator(); it.hasNext();) {
			Object key = it.next();
			sb.append(key);
			sb.append('=');
			sb.append(get(key));
			if (it.hasNext()) {
				sb.append(", ");
			}
		}
		sb.append('}');
		return sb.toString();
	}
}
