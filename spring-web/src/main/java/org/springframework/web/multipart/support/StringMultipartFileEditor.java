/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.multipart.support;

import java.beans.PropertyEditorSupport;
import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.web.multipart.MultipartFile;

/**
 * Custom {@link java.beans.PropertyEditor} for converting
 * {@link MultipartFile MultipartFiles} to Strings.
 *
 * <p>Allows one to specify the charset to use.
 *
 * @author Juergen Hoeller
 * @since 13.10.2003
 */
public class StringMultipartFileEditor extends PropertyEditorSupport {

	private final @Nullable String charsetName;


	/**
	 * Create a new {@link StringMultipartFileEditor}, using the default charset.
	 */
	public StringMultipartFileEditor() {
		this.charsetName = null;
	}

	/**
	 * Create a new {@link StringMultipartFileEditor}, using the given charset.
	 * @param charsetName valid charset name
	 * @see java.lang.String#String(byte[],String)
	 */
	public StringMultipartFileEditor(String charsetName) {
		this.charsetName = charsetName;
	}


	@Override
	public void setAsText(String text) {
		setValue(text);
	}

	@Override
	public void setValue(Object value) {
		if (value instanceof MultipartFile multipartFile) {
			try {
				super.setValue(this.charsetName != null ?
						new String(multipartFile.getBytes(), this.charsetName) :
						new String(multipartFile.getBytes()));
			}
			catch (IOException ex) {
				throw new IllegalArgumentException("Cannot read contents of multipart file", ex);
			}
		}
		else {
			super.setValue(value);
		}
	}

}
