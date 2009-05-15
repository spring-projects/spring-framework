package org.springframework.core.convert.converter;

public interface ConverterRegistry {
	void addConverter(Converter<?, ?> converter);

	void addConverterFactory(ConverterFactory<?, ?> converter);

	void removeConverter(Converter<?, ?> converter);

	void removeConverterFactory(Converter<?, ?> converter);

}