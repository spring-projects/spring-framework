/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.nio.charset.Charset;

import org.springframework.util.StringUtils;

/**
 * Editor for {@code java.nio.charset.Charset}, translating charset
 * String representations into Charset objects and back.
 *
 * <p>Expects the same syntax as Charset's {@link java.nio.charset.Charset#name()},
 * for example, {@code UTF-8}, {@code ISO-8859-16}, etc.
 *
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 2.5.4
 * @see Charset
 */
public class CharsetEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			setValue(Charset.forName(text.trim()));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Charset value = (Charset) getValue();
		return (value != null ? value.name() : "");
	}

}
