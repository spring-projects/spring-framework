package org.springframework.core.convert.converter;

import org.springframework.core.convert.service.DefaultConversionService;

/**
 * Simply calls {@link Object#toString()} to convert any object to a string.
 * Used by the {@link DefaultConversionService} as a fallback if there are no other explicit to string converters registered.
 * @author Keith Donald
 */
public class ObjectToString implements SuperConverter<Object, String> {

	@SuppressWarnings("unchecked")
	public <RT extends String> RT convert(Object source, Class<RT> targetClass) {
		return (RT) source.toString();
	}
	
}
