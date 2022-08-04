/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.mail.javamail;

import java.beans.PropertyEditorSupport;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

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
 * @see javax.mail.internet.InternetAddress
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
