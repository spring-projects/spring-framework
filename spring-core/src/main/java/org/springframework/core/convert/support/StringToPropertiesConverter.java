/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.convert.support;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.springframework.core.convert.converter.Converter;

/**
 * Converts a String to a Properties by calling Properties#load(java.io.InputStream).
 * Uses ISO-8559-1 encoding required by Properties.
 *
 * @author Keith Donald
 * @since 3.0
 */
final class StringToPropertiesConverter implements Converter<String, Properties> {

	@Override
	public Properties convert(String source) {
		try {
			Properties props = new Properties();
			// Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
			props.load(new ByteArrayInputStream(source.getBytes(StandardCharsets.ISO_8859_1)));
			return props;
		}
		catch (Exception ex) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to parse [" + source + "] into Properties", ex);
		}
	}

}
