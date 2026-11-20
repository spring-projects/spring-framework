/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.tags.form;

import java.beans.PropertyEditorSupport;

/**
 * @author Rob Harrop
 * @since 2.0
 */
class SimpleFloatEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(Float.valueOf(text));
	}

	@Override
	public String getAsText() {
		return getValue() + "f";
	}
}
