package org.springframework.core.convert.converter;

/**
 * Adapts a {@link SuperTwoWayConverter} to the {@link Converter} interface in a type safe way. This adapter is useful
 * for applying more general {@link SuperConverter} logic to a specific source/target class pair.
 */
@SuppressWarnings("unchecked")
public class SuperTwoWayConverterConverter<S, T> implements Converter<S, T> {

	private SuperTwoWayConverter superConverter;

	private Class sourceClass;

	private Class targetClass;

	public <SCS, SCT> SuperTwoWayConverterConverter(SuperTwoWayConverter<SCS, SCT> superConverter,
			Class<? extends SCS> sourceClass, Class<? extends SCT> targetClass) {
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
