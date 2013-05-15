/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.io.ByteArrayInputStream;
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
			props.load(new ByteArrayInputStream(source.getBytes("ISO-8859-1")));
			return props;
		}
		catch (Exception ex) {
			// Should never happen.
			throw new IllegalArgumentException("Failed to parse [" + source + "] into Properties", ex);
		}
	}

}
