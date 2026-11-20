/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

/**
 * Custom {@link java.beans.PropertyEditor} for {@link Properties} objects.
 *
 * <p>Handles conversion from content {@link String} to {@code Properties} object.
 * Also handles {@link Map} to {@code Properties} conversion, for populating
 * a {@code Properties} object via XML "map" entries.
 *
 * <p>The required format is defined in the standard {@code Properties}
 * documentation. Each property must be on a new line.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see java.util.Properties#load
 */
public class PropertiesEditor extends PropertyEditorSupport {

	/**
	 * Convert {@link String} into {@link Properties}, considering it as
	 * properties content.
	 * @param text the text to be so converted
	 */
	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		Properties props = new Properties();
		if (text != null) {
			try {
				// Must use the ISO-8859-1 encoding because Properties.load(stream) expects it.
				props.load(new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1)));
			}
			catch (IOException ex) {
				// Should never happen.
				throw new IllegalArgumentException(
						"Failed to parse [" + text + "] into Properties", ex);
			}
		}
		setValue(props);
	}

	/**
	 * Take {@link Properties} as-is; convert {@link Map} into {@code Properties}.
	 */
	@Override
	public void setValue(Object value) {
		if (!(value instanceof Properties) && value instanceof Map<?, ?> map) {
			Properties props = new Properties();
			props.putAll(map);
			super.setValue(props);
		}
		else {
			super.setValue(value);
		}
	}

}
