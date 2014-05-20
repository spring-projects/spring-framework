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

import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.objenesis.ObjenesisStd;
import org.springframework.objenesis.instantiator.ObjectInstantiator;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link PropertyAccessor} implementation that directly accesses instance fields.
 * Allows for direct binding to fields instead of going through JavaBean setters.
 *
 * <p>Since 4.1 this implementation supports nested fields traversing.
 *
 * <p>A DirectFieldAccessor's default for the "extractOldValueForEditor" setting
 * is "true", since a field can always be read without side effects.
 *
 * @author Juergen Hoeller
 * @author Maciej Walkowiak
 * @since 2.0
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see org.springframework.validation.DirectFieldBindingResult
 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
 */
public class DirectFieldAccessor extends AbstractPropertyAccessor {

	private final Object target;

	private final Map<String, FieldHolder> fieldMap = new HashMap<String, FieldHolder>();

	/**
	 * Create a new DirectFieldAccessor for the given target object.
	 * @param target the target object to access
	 */
	public DirectFieldAccessor(final Object target) {
		Assert.notNull(target, "Target object must not be null");
		this.target = target;

		this.typeConverterDelegate = new TypeConverterDelegate(this, target);
		registerDefaultEditors();
		setExtractOldValueForEditor(true);
	}

	@Override
	public boolean isReadableProperty(String propertyName) throws BeansException {
		return doesPropertyExists(propertyName);
	}

	@Override
	public boolean isWritableProperty(String propertyName) throws BeansException {
		return doesPropertyExists(propertyName);
	}

	@Override
	public Class<?> getPropertyType(String propertyPath) throws BeansException {
		FieldHolder fieldHolder = getFieldHolder(propertyPath);
		if (fieldHolder != null) {
			return fieldHolder.getField().getType();
		}
		return null;
	}

	@Override
	public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
		try {
			FieldHolder fieldHolder = getFieldHolder(propertyName);
			return new TypeDescriptor(fieldHolder.getField());
		} catch (InvalidPropertyException e) {
			return null;
		}
	}

	@Override
	public Object getPropertyValue(String propertyName) throws BeansException {
		FieldHolder fieldHolder;

		try {
			fieldHolder = this.getFieldHolder(propertyName);
		} catch (InvalidPropertyException ex) {
			throw new NotReadablePropertyException(ex.getBeanClass(), ex.getPropertyName(), ex.getMessage());
		}

		return fieldHolder.getValue();
	}

	@Override
	public void setPropertyValue(String propertyName, Object newValue) throws BeansException {
		FieldHolder fieldHolder;

		try {
			fieldHolder = this.getFieldHolder(propertyName);
		} catch (InvalidPropertyException ex) {
			throw new NotWritablePropertyException(ex.getBeanClass(), ex.getPropertyName(), ex.getMessage());
		}

		Object oldValue = null;
		try {
			oldValue = fieldHolder.getValue();

			Object convertedValue = this.typeConverterDelegate.convertIfNecessary(
					fieldHolder.getField().getName(), fieldHolder.getValue(), newValue, fieldHolder.getField().getType(),
					new TypeDescriptor(fieldHolder.getField()));

			fieldHolder.setValue(convertedValue);
		}
		catch (ConverterNotFoundException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(fieldHolder.getTarget(), propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, fieldHolder.getField().getType(), ex);
		}
		catch (ConversionException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(fieldHolder.getTarget(), propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, fieldHolder.getField().getType(), ex);
		}
		catch (IllegalStateException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(fieldHolder.getTarget(), propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, fieldHolder.getField().getType(), ex);
		}
		catch (IllegalArgumentException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(fieldHolder.getTarget(), propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, fieldHolder.getField().getType(), ex);
		}
	}

	private boolean doesPropertyExists(String propertyName) {
		try {
			FieldHolder fieldHolder = getFieldHolder(propertyName);
			return fieldHolder != null;
		} catch (InvalidPropertyException e) {
			return false;
		}
	}

	/**
	 * Gets FieldHolder for given property name.
	 *
	 * @param propertyName - property name, can be simple field name or path containing dots, for example "person.address.city"
	 * @return FieldHolder when corresponding field found on target object
	 * @throws org.springframework.beans.InvalidPropertyException when field not found
	 */
	private FieldHolder getFieldHolder(String propertyName) {
		if (!fieldMap.containsKey(propertyName)) {
			try {
				fieldMap.put(propertyName, getField(propertyName, target));
			} catch (IllegalAccessException e) {
				throw new InvalidPropertyException(this.target.getClass(), propertyName, "Field is not accessible", e);
			}
		}

		return fieldMap.get(propertyName);
	}

	private FieldHolder getField(String propertyName, Object target) throws IllegalAccessException {
		final boolean hasNestedProperties = propertyName.contains(".");

		String property = hasNestedProperties ? propertyName.substring(0, propertyName.indexOf(".")) : propertyName;

		Field field = ReflectionUtils.findField(target.getClass(), property);

		if (field == null) {
			throw new InvalidPropertyException(target.getClass(), property, "Field does not exists");
		}

		ReflectionUtils.makeAccessible(field);

		if (hasNestedProperties) {
			Object newTarget = field.get(target);

			if (newTarget == null) {
				// nested object not created, creating new instance using default constructor
				ObjenesisStd objenesis = new ObjenesisStd();
				ObjectInstantiator<?> instantiator = objenesis.getInstantiatorOf(field.getType());

				newTarget = instantiator.newInstance();
				ReflectionUtils.setField(field, target, newTarget);
			}

			return getField(propertyName.substring(property.length() + 1), newTarget);
		} else {
			return new FieldHolder(target, field);
		}

	}

	/**
	 * Wraps field and corresponding target object together. Provides simple methods for getting and setting value
	 * from field on target object.
	 */
	private class FieldHolder {
		private final Object target;
		private final Field field;

		private FieldHolder(Object target, Field field) {
			Assert.notNull(target);
			Assert.notNull(field);
			this.target = target;
			this.field = field;
		}

		public Object getTarget() {
			return target;
		}

		public Field getField() {
			return field;
		}

		public Object getValue() {
			try {
				return this.field.get(this.target);
			} catch (IllegalAccessException e) {
				throw new InvalidPropertyException(this.target.getClass(), this.field.getName(), "Field is not accessible", e);
			}
		}

		public void setValue(Object value) {
			try {
				this.field.set(this.target, value);
			} catch (IllegalAccessException e) {
				throw new InvalidPropertyException(this.target.getClass(), this.field.getName(), "Field is not accessible", e);
			}
		}
	}
}
