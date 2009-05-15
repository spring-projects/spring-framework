package org.springframework.core.convert.converter;

/**
 * A interface for registering converters with a type conversion system.
 * @author Keith Donald
 */
public interface ConverterRegistry {
	
	/**
	 * Add a converter to this registry.
	 */
	void addConverter(Converter<?, ?> converter);

	/**
	 * Add a converter factory to this registry.
	 */
	void addConverterFactory(ConverterFactory<?, ?> converter);

	/**
	 * Remove a converter from this registry.
	 */
	void removeConverter(Converter<?, ?> converter);

	/**
	 * Remove a converter factory from this registry.
	 */
	void removeConverterFactory(Converter<?, ?> converter);

}