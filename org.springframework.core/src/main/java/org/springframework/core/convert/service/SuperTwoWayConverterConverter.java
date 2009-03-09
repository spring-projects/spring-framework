package org.springframework.core.convert.service;

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.SuperConverter;
import org.springframework.core.convert.converter.SuperTwoWayConverter;

/**
 * Adapts a {@link SuperTwoWayConverter} to the {@link Converter} interface in a type safe way. This adapter is useful
 * for applying more general {@link SuperConverter} logic to a specific source/target class pair.
 */
@SuppressWarnings("unchecked")
class SuperTwoWayConverterConverter<S, T> implements Converter<S, T> {

	private SuperTwoWayConverter superConverter;

	private Class sourceClass;

	private Class targetClass;

	public SuperTwoWayConverterConverter(SuperTwoWayConverter superConverter, Class sourceClass, Class targetClass) {
		this.superConverter = superConverter;
		this.sourceClass = sourceClass;
		this.targetClass = targetClass;
	}

	public T convert(S source) throws Exception {
		return (T) superConverter.convert(source, targetClass);
	}

	public S convertBack(T target) throws Exception {
		return (S) superConverter.convertBack(target, sourceClass);
	}

}
