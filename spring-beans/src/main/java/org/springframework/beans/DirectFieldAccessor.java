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

package org.springframework.beans;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link PropertyAccessor} implementation that directly accesses instance fields.
 * Allows for direct binding to fields instead of going through JavaBean setters.
 *
 * <p>As of Spring 4.1, this implementation supports nested field traversal.
 *
 * <p>A DirectFieldAccessor's default for the "extractOldValueForEditor" setting
 * is "true", since a field can always be read without side effects.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see org.springframework.validation.DirectFieldBindingResult
 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
 */
public class DirectFieldAccessor extends AbstractPropertyAccessor {

	private final Object rootObject;

	private final Map<String, FieldAccessor> fieldMap = new HashMap<String, FieldAccessor>();


	/**
	 * Create a new DirectFieldAccessor for the given root object.
	 * @param rootObject the root object to access
	 */
	public DirectFieldAccessor(final Object rootObject) {
		Assert.notNull(rootObject, "Root object must not be null");
		this.rootObject = rootObject;
		this.typeConverterDelegate = new TypeConverterDelegate(this, rootObject);
		registerDefaultEditors();
		setExtractOldValueForEditor(true);
	}

	/**
	 * Return the root object at the top of the path of this instance.
	 */
	public final Object getRootInstance() {
		return this.rootObject;
	}

	/**
	 * Return the class of the root object at the top of the path of this instance.
	 */
	public final Class<?> getRootClass() {
		return (this.rootObject != null ? this.rootObject.getClass() : null);
	}

	@Override
	public boolean isReadableProperty(String propertyName) throws BeansException {
		return hasProperty(propertyName);
	}

	@Override
	public boolean isWritableProperty(String propertyName) throws BeansException {
		return hasProperty(propertyName);
	}

	@Override
	public Class<?> getPropertyType(String propertyPath) throws BeansException {
		FieldAccessor fieldAccessor = getFieldAccessor(propertyPath);
		if (fieldAccessor != null) {
			return fieldAccessor.getField().getType();
		}
		return null;
	}

	@Override
	public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
		FieldAccessor fieldAccessor = getFieldAccessor(propertyName);
		if (fieldAccessor != null) {
			return new TypeDescriptor(fieldAccessor.getField());
		}
		return null;
	}

	@Override
	public Object getPropertyValue(String propertyName) throws BeansException {
		FieldAccessor fieldAccessor = getFieldAccessor(propertyName);
		if (fieldAccessor == null) {
			throw new NotReadablePropertyException(
					getRootClass(), propertyName, "Field '" + propertyName + "' does not exist");
		}
		return fieldAccessor.getValue();
	}

	@Override
	public void setPropertyValue(String propertyName, Object newValue) throws BeansException {
		FieldAccessor fieldAccessor = getFieldAccessor(propertyName);
		if (fieldAccessor == null) {
			throw new NotWritablePropertyException(
					getRootClass(), propertyName, "Field '" + propertyName + "' does not exist");
		}
		Field field = fieldAccessor.getField();
		Object oldValue = null;
		try {
			oldValue = fieldAccessor.getValue();
			Object convertedValue = this.typeConverterDelegate.convertIfNecessary(
					field.getName(), oldValue, newValue, field.getType(), new TypeDescriptor(field));
			fieldAccessor.setValue(convertedValue);
		}
		catch (ConverterNotFoundException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(getRootInstance(), propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, field.getType(), ex);
		}
		catch (ConversionException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(getRootInstance(), propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, field.getType(), ex);
		}
		catch (IllegalStateException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(getRootInstance(), propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, field.getType(), ex);
		}
		catch (IllegalArgumentException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(getRootInstance(), propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, field.getType(), ex);
		}
	}

	private boolean hasProperty(String propertyPath) {
		Assert.notNull(propertyPath, "PropertyPath must not be null");
		return getFieldAccessor(propertyPath) != null;
	}

	private FieldAccessor getFieldAccessor(String propertyPath) {
		FieldAccessor fieldAccessor = this.fieldMap.get(propertyPath);
		if (fieldAccessor == null) {
			fieldAccessor = doGetFieldAccessor(propertyPath, getRootClass());
			this.fieldMap.put(propertyPath, fieldAccessor);
		}
		return fieldAccessor;
	}

	private FieldAccessor doGetFieldAccessor(String propertyPath, Class<?> targetClass) {
		StringTokenizer st = new StringTokenizer(propertyPath, ".");
		FieldAccessor accessor = null;
		Class<?> parentType = targetClass;
		while (st.hasMoreTokens()) {
			String localProperty = st.nextToken();
			Field field = ReflectionUtils.findField(parentType, localProperty);
			if (field == null) {
				return null;
			}
			if (accessor == null) {
				accessor = root(propertyPath, localProperty, field);
			}
			else {
				accessor = accessor.child(localProperty, field);
			}
			parentType = field.getType();
		}
		return accessor;
	}

	/**
	 * Create a root {@link FieldAccessor}.
	 * @param canonicalName the full expression for the field to access
	 * @param actualName the name of the local (root) property
	 * @param field the field accessing the property
	 */
	private FieldAccessor root(String canonicalName, String actualName, Field field) {
		return new FieldAccessor(null, canonicalName, actualName, field);
	}


	/**
	 * Provide an easy access to a potentially hierarchical value.
	 */
	private class FieldAccessor {

		private final List<FieldAccessor> parents;

		private final String canonicalName;

		private final String actualName;

		private final Field field;

		/**
		 * Create a new FieldAccessor instance.
		 * @param parent the parent accessor, if any
		 * @param canonicalName the full expression for the field to access
		 * @param actualName the name of the partial expression for this property
		 * @param field the field accessing the property
		 */
		public FieldAccessor(FieldAccessor parent, String canonicalName, String actualName, Field field) {
			Assert.notNull(canonicalName, "Expression must no be null");
			Assert.notNull(field, "Field must no be null");
			this.parents = buildParents(parent);
			this.canonicalName = canonicalName;
			this.actualName = actualName;
			this.field = field;
		}

		/**
		 * Create a child instance.
		 * @param actualName the name of the child property
		 * @param field the field accessing the child property
		 */
		public FieldAccessor child(String actualName, Field field) {
			return new FieldAccessor(this, this.canonicalName, this.actualName + "." + actualName, field);
		}

		public Field getField() {
			return this.field;
		}

		public Object getValue() {
			Object localTarget = getLocalTarget(getRootInstance());
			return getParentValue(localTarget);

		}

		public void setValue(Object value) {
			Object localTarget = getLocalTarget(getRootInstance());
			try {
				this.field.set(localTarget, value);
			}
			catch (IllegalAccessException ex) {
				throw new InvalidPropertyException(localTarget.getClass(), canonicalName, "Field is not accessible", ex);
			}
		}

		private Object getParentValue(Object target) {
			try {
				ReflectionUtils.makeAccessible(this.field);
				return this.field.get(target);
			}
			catch (IllegalAccessException ex) {
				throw new InvalidPropertyException(target.getClass(),
						this.canonicalName, "Field is not accessible", ex);
			}
		}

		private Object getLocalTarget(Object rootTarget) {
			Object localTarget = rootTarget;
			for (FieldAccessor parent : parents) {
				localTarget = autoGrowIfNecessary(parent, parent.getParentValue(localTarget));
				if (localTarget == null) { // Could not traverse the graph any further
					throw new NullValueInNestedPathException(getRootClass(), parent.actualName,
							"Cannot access indexed value of property referenced in indexed property path '" +
									getField().getName() + "': returned null");
				}
			}
			return localTarget;
		}

		private Object newValue() {
			Class<?> type = getField().getType();
			try {
				return type.newInstance();
			}
			catch (Exception ex) {
				throw new NullValueInNestedPathException(getRootClass(), this.actualName,
						"Could not instantiate property type [" + type.getName() +
								"] to auto-grow nested property path: " + ex);
			}
		}

		private Object autoGrowIfNecessary(FieldAccessor accessor, Object value) {
			if (value == null && isAutoGrowNestedPaths()) {
				Object defaultValue = accessor.newValue();
				accessor.setValue(defaultValue);
				return defaultValue;
			}
			return value;
		}

		private List<FieldAccessor> buildParents(FieldAccessor parent) {
			List<FieldAccessor> parents = new ArrayList<FieldAccessor>();
			if (parent != null) {
				parents.addAll(parent.parents);
				parents.add(parent);
			}
			return parents;
		}
	}

}
