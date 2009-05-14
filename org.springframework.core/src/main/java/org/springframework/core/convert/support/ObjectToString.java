package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;

/**
 * Simply calls {@link Object#toString()} to convert any object to a string.
 * Used by the {@link DefaultConversionService} as a fallback if there are no other explicit to string converters registered.
 * @author Keith Donald
 */
public class ObjectToString implements Converter<Object, String> {
	public String convert(Object source) {
		return source.toString();
	}
}
