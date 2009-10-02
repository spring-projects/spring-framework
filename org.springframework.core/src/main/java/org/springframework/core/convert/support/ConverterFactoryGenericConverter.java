/**
 * 
 */
package org.springframework.core.convert.support;

import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConverterFactory;

/**
 * Adapts a ConverterFactory to the uniform GenericConverter interface.
 * @author Keith Donald 
 */
@SuppressWarnings("unchecked")
public final class ConverterFactoryGenericConverter implements GenericConverter {

	private final ConverterFactory converterFactory;

	public ConverterFactoryGenericConverter(ConverterFactory converterFactory) {
		this.converterFactory = converterFactory;
	}

	public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
		return this.converterFactory.getConverter(targetType.getObjectType()).convert(source);
	}
}