package org.springframework.context.conversionservice;

import org.springframework.core.convert.converter.Converter;

public class StringToBarConverter implements Converter<String, Bar> {

	public Bar convert(String source) {
		return new Bar(source);
	}

}
