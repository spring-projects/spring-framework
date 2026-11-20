/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart.support;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.propertyeditors.ByteArrayPropertyEditor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Custom {@link java.beans.PropertyEditor} for converting
 * {@link MultipartFile MultipartFiles} to byte arrays.
 *
 * @author Juergen Hoeller
 * @since 13.10.2003
 */
public class ByteArrayMultipartFileEditor extends ByteArrayPropertyEditor {

	@Override
	public void setValue(@Nullable Object value) {
		if (value instanceof MultipartFile multipartFile) {
			try {
				super.setValue(multipartFile.getBytes());
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
			}
		}
		else if (value instanceof byte[]) {
			super.setValue(value);
		}
		else {
			super.setValue(value != null ? value.toString().getBytes() : null);
		}
	}

	@Override
	public String getAsText() {
		byte[] value = (byte[]) getValue();
		return (value != null ? new String(value) : "");
	}

}
