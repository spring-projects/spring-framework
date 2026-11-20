/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.time.DateTimeException;
import java.time.ZoneId;

import org.springframework.util.StringUtils;

/**
 * Editor for {@code java.time.ZoneId}, translating time zone Strings into {@code ZoneId}
 * objects. Exposes the time zone as a text representation.
 *
 * @author Nicholas Williams
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 4.0
 * @see java.time.ZoneId
 * @see TimeZoneEditor
 */
public class ZoneIdEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			text = text.trim();
		}
		try {
			setValue(ZoneId.of(text));
		}
		catch (DateTimeException ex) {
			throw new IllegalArgumentException(ex.getMessage(), ex);
		}
	}

	@Override
	public String getAsText() {
		ZoneId value = (ZoneId) getValue();
		return (value != null ? value.getId() : "");
	}

}
