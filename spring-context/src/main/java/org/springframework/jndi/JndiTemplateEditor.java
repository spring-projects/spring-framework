/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jndi;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.propertyeditors.PropertiesEditor;

/**
 * Properties editor for JndiTemplate objects. Allows properties of type
 * JndiTemplate to be populated with a properties-format string.
 *
 * @author Rod Johnson
 * @since 09.05.2003
 */
public class JndiTemplateEditor extends PropertyEditorSupport {

	private final PropertiesEditor propertiesEditor = new PropertiesEditor();

	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		if (text == null) {
			throw new IllegalArgumentException("JndiTemplate cannot be created from null string");
		}
		if (text.isEmpty()) {
			// empty environment
			setValue(new JndiTemplate());
		}
		else {
			// we have a non-empty properties string
			this.propertiesEditor.setAsText(text);
			Properties props = (Properties) this.propertiesEditor.getValue();
			setValue(new JndiTemplate(props));
		}
	}

}
