package org.springframework.core.convert.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterInfo;
import org.springframework.core.convert.converter.SuperConverter;
import org.springframework.core.convert.converter.SuperTwoWayConverter;

/**
 * Adapts a {@link SuperTwoWayConverter} to the {@link Converter} interface in a type safe way. This adapter is useful
 * for applying more general {@link SuperConverter} logic to a specific source/target class pair.
 */
@SuppressWarnings("unchecked")
class SuperTwoWayConverterConverter implements Converter, ConverterInfo {

	private SuperTwoWayConverter superConverter;

	private Class sourceType;

	private Class targetType;

	public SuperTwoWayConverterConverter(SuperTwoWayConverter superConverter, Class sourceType, Class targetType) {
		this.superConverter = superConverter;
		this.sourceType = sourceType;
		this.targetType = targetType;
	}
	
	public Class getSourceType() {
		return sourceType;
	}

	public Class getTargetType() {
		return targetType;
	}

	public Object convert(Object source) throws Exception {
		return superConverter.convert(source, targetType);
	}

	public Object convertBack(Object target) throws Exception {
		return superConverter.convertBack(target, sourceType);
	}

}
