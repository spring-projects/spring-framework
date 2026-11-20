/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans;

import java.beans.PropertyEditorSupport;
import java.util.Properties;

import org.springframework.beans.propertyeditors.PropertiesEditor;

/**
 * {@link java.beans.PropertyEditor Editor} for a {@link PropertyValues} object.
 *
 * <p>The required format is defined in the {@link java.util.Properties}
 * documentation. Each property must be on a new line.
 *
 * <p>The present implementation relies on a
 * {@link org.springframework.beans.propertyeditors.PropertiesEditor}
 * underneath.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public class PropertyValuesEditor extends PropertyEditorSupport {

	private final PropertiesEditor propertiesEditor = new PropertiesEditor();

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		this.propertiesEditor.setAsText(text);
		Properties props = (Properties) this.propertiesEditor.getValue();
		setValue(new MutablePropertyValues(props));
	}

}

