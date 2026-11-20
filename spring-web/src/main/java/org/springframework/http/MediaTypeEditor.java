/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http;

import java.beans.PropertyEditorSupport;

import org.springframework.util.StringUtils;

/**
 * {@link java.beans.PropertyEditor Editor} for {@link MediaType}
 * descriptors, to automatically convert {@code String} specifications
 * (for example, {@code "text/html"}) to {@code MediaType} properties.
 *
 * @author Juergen Hoeller
 * @since 3.0
 * @see MediaType
 */
public class MediaTypeEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) {
		if (StringUtils.hasText(text)) {
			setValue(MediaType.parseMediaType(text));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		MediaType mediaType = (MediaType) getValue();
		return (mediaType != null ? mediaType.toString() : "");
	}

}
