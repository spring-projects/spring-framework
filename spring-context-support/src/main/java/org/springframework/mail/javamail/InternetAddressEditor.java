/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mail.javamail;

import java.beans.PropertyEditorSupport;

import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.springframework.util.StringUtils;

/**
 * Editor for {@code java.mail.internet.InternetAddress},
 * to directly populate an InternetAddress property.
 *
 * <p>Expects the same syntax as InternetAddress's constructor with
 * a String argument. Converts empty Strings into null values.
 *
 * @author Juergen Hoeller
 * @since 1.2.3
 * @see jakarta.mail.internet.InternetAddress
 */
public class InternetAddressEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			try {
				setValue(new InternetAddress(text));
			}
			catch (AddressException ex) {
				throw new IllegalArgumentException("Could not parse mail address: " + ex.getMessage());
			}
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		InternetAddress value = (InternetAddress) getValue();
		return (value != null ? value.toUnicodeString() : "");
	}

}
