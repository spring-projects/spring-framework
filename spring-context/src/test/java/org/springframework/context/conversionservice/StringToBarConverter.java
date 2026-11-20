/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.conversionservice;

import org.springframework.core.convert.converter.Converter;

/**
 * @author Keith Donald
 */
public class StringToBarConverter implements Converter<String, Bar> {

	@Override
	public Bar convert(String source) {
		return new Bar(source);
	}

}
