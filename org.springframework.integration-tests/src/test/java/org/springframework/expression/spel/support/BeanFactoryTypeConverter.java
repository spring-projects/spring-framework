package org.springframework.expression.spel.support;
import java.beans.PropertyEditor;

import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.expression.TypeConverter;

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
				defaultConversionService = ConversionServiceFactory.createDefaultConversionService();
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

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableBeanFactory) {
			Object typeConverter = ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
			if (typeConverter instanceof SimpleTypeConverter) {
				delegate = (SimpleTypeConverter) typeConverter;
			}
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

	public boolean canConvert(TypeDescriptor sourceTypeDescriptor, TypeDescriptor targetTypeDescriptor) {
		if (conversionService.canConvert(sourceTypeDescriptor, targetTypeDescriptor)) {
			return true;
		}
		// TODO: what does this mean? This method is not used in SpEL so probably ignorable?
		Class<?> sourceType = sourceTypeDescriptor.getObjectType();
		Class<?> targetType = targetTypeDescriptor.getObjectType();
		return canConvert(sourceType, targetType);
	}

	public Object convertValue(Object value, TypeDescriptor sourceType, TypeDescriptor targetType) {
		if (targetType.getType() == Void.class || targetType.getType() == Void.TYPE) {
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