package org.springframework.mapping.support;

import org.springframework.core.convert.converter.Converter;

public interface MappingConfiguration {
	MappingConfiguration setConverter(Converter converter);
}