/*
 * Copyright 2002-present the original author or authors.
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

import java.beans.PropertyEditor;

import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.expression.TypeConverter;
import org.springframework.util.ClassUtils;

/**
 * Copied from Spring Integration for purposes of reproducing
 * {@link Spr7538Tests}.
 */
class BeanFactoryTypeConverter implements TypeConverter, BeanFactoryAware {

	private SimpleTypeConverter delegate = new SimpleTypeConverter();

	private static ConversionService defaultConversionService;

	private ConversionService conversionService;

	public BeanFactoryTypeConverter() {
		synchronized (this) {
			if (defaultConversionService == null) {
				defaultConversionService = new DefaultConversionService();
			}
		}
		this.conversionService = defaultConversionService;
	}

	public BeanFactoryTypeConverter(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public void setConversionService(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableBeanFactory cbf &&
				cbf.getTypeConverter() instanceof SimpleTypeConverter simpleTypeConverter) {
			this.delegate = simpleTypeConverter;
		}
	}

	public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
		if (conversionService.canConvert(sourceType, targetType)) {
			return true;
		}
		if (!String.class.isAssignableFrom(sourceType) && !String.class.isAssignableFrom(targetType)) {
			// PropertyEditor cannot convert non-Strings
			return false;
		}
		if (!String.class.isAssignableFrom(sourceType)) {
			return delegate.findCustomEditor(sourceType, null) != null || delegate.getDefaultEditor(sourceType) != null;
		}
		return delegate.findCustomEditor(targetType, null) != null || delegate.getDefaultEditor(targetType) != null;
	}

	@Override
	public boolean canConvert(TypeDescriptor sourceTypeDescriptor, TypeDescriptor targetTypeDescriptor) {
		if (conversionService.canConvert(sourceTypeDescriptor, targetTypeDescriptor)) {
			return true;
		}
		Class<?> sourceType = sourceTypeDescriptor.getObjectType();
		Class<?> targetType = targetTypeDescriptor.getObjectType();
		return canConvert(sourceType, targetType);
	}

	@Override
	public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (ClassUtils.isVoidType(targetType.getType())) {
			return null;
		}
		if (conversionService.canConvert(sourceType, targetType)) {
			return conversionService.convert(value, sourceType, targetType);
		}
		if (!String.class.isAssignableFrom(sourceType.getType())) {
			PropertyEditor editor = delegate.findCustomEditor(sourceType.getType(), null);
			editor.setValue(value);
			return editor.getAsText();
		}
		return delegate.convertIfNecessary(value, targetType.getType());
	}

}
