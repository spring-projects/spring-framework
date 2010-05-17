/*
 * Copyright 2002-2010 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionException;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@link PropertyAccessor} implementation that directly accesses instance fields.
 * Allows for direct binding to fields instead of going through JavaBean setters.
 *
 * <p>This implementation just supports fields in the actual target object.
 * It is not able to traverse nested fields.
 *
 * <p>A DirectFieldAccessor's default for the "extractOldValueForEditor" setting
 * is "true", since a field can always be read without side effects.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see org.springframework.validation.DirectFieldBindingResult
 * @see org.springframework.validation.DataBinder#initDirectFieldAccess()
 */
public class DirectFieldAccessor extends AbstractPropertyAccessor {

	private final Object target;

	private final Map<String, Field> fieldMap = new HashMap<String, Field>();

	private final TypeConverterDelegate typeConverterDelegate;


	/**
	 * Create a new DirectFieldAccessor for the given target object.
	 * @param target the target object to access
	 */
	public DirectFieldAccessor(Object target) {
		Assert.notNull(target, "Target object must not be null");
		this.target = target;
		ReflectionUtils.doWithFields(this.target.getClass(), new ReflectionUtils.FieldCallback() {
			public void doWith(Field field) {
				fieldMap.put(field.getName(), field);
			}
		});
		this.typeConverterDelegate = new TypeConverterDelegate(this, target);
		registerDefaultEditors();
		setExtractOldValueForEditor(true);
	}


	public boolean isReadableProperty(String propertyName) throws BeansException {
		return this.fieldMap.containsKey(propertyName);
	}

	public boolean isWritableProperty(String propertyName) throws BeansException {
		return this.fieldMap.containsKey(propertyName);
	}

	@Override
	public Class getPropertyType(String propertyName) throws BeansException {
		Field field = this.fieldMap.get(propertyName);
		if (field != null) {
			return field.getType();
		}
		return null;
	}

	public TypeDescriptor getPropertyTypeDescriptor(String propertyName) throws BeansException {
		Field field = this.fieldMap.get(propertyName);
		if (field != null) {
			return new TypeDescriptor(field);
		}
		return null;
	}

	@Override
	public Object getPropertyValue(String propertyName) throws BeansException {
		Field field = this.fieldMap.get(propertyName);
		if (field == null) {
			throw new NotReadablePropertyException(
					this.target.getClass(), propertyName, "Field '" + propertyName + "' does not exist");
		}
		try {
			ReflectionUtils.makeAccessible(field);
			return field.get(this.target);
		}
		catch (IllegalAccessException ex) {
			throw new InvalidPropertyException(this.target.getClass(), propertyName, "Field is not accessible", ex);
		}
	}

	@Override
	public void setPropertyValue(String propertyName, Object newValue) throws BeansException {
		Field field = this.fieldMap.get(propertyName);
		if (field == null) {
			throw new NotWritablePropertyException(
					this.target.getClass(), propertyName, "Field '" + propertyName + "' does not exist");
		}
		Object oldValue = null;
		try {
			ReflectionUtils.makeAccessible(field);
			oldValue = field.get(this.target);
			Object convertedValue = this.typeConverterDelegate.convertIfNecessary(
					field.getName(), oldValue, newValue, field.getType(), new TypeDescriptor(field));
			field.set(this.target, convertedValue);
		}
		catch (ConverterNotFoundException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this.target, propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, field.getType(), ex);
		}
		catch (ConversionException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this.target, propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, field.getType(), ex);
		}
		catch (IllegalStateException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this.target, propertyName, oldValue, newValue);
			throw new ConversionNotSupportedException(pce, field.getType(), ex);
		}
		catch (IllegalArgumentException ex) {
			PropertyChangeEvent pce = new PropertyChangeEvent(this.target, propertyName, oldValue, newValue);
			throw new TypeMismatchException(pce, field.getType(), ex);
		}
		catch (IllegalAccessException ex) {
			throw new InvalidPropertyException(this.target.getClass(), propertyName, "Field is not accessible", ex);
		}
	}

	public <T> T convertIfNecessary(
			Object value, Class<T> requiredType, MethodParameter methodParam) throws TypeMismatchException {
		try {
			return this.typeConverterDelegate.convertIfNecessary(value, requiredType, methodParam);
		}
		catch (IllegalArgumentException ex) {
			throw new TypeMismatchException(value, requiredType, ex);
		}
		catch (IllegalStateException ex) {
			throw new ConversionNotSupportedException(value, requiredType, ex);
		}
	}

}
