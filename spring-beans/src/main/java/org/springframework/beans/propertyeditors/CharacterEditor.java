/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.beans.propertyeditors;

import java.beans.PropertyEditorSupport;
import java.util.HexFormat;

import org.jspecify.annotations.Nullable;

import org.springframework.util.StringUtils;

/**
 * Editor for a {@link Character}, to populate a property
 * of type {@code Character} or {@code char} from a String value.
 *
 * <p>Note that the JDK does not contain a default
 * {@link java.beans.PropertyEditor property editor} for {@code char}!
 * {@link org.springframework.beans.BeanWrapperImpl} will register this
 * editor by default.
 *
 * <p>Also supports conversion from a Unicode character sequence; for example,
 * {@code u0041} ('A').
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 * @since 1.2
 * @see Character
 * @see org.springframework.beans.BeanWrapperImpl
 */
public class CharacterEditor extends PropertyEditorSupport {

	/**
	 * The prefix that identifies a string as being a Unicode character sequence.
	 */
	private static final String UNICODE_PREFIX = "\\u";

	/**
	 * The length of a Unicode character sequence.
	 */
	private static final int UNICODE_LENGTH = 6;


	private final boolean allowEmpty;


	/**
	 * Create a new CharacterEditor instance.
	 * <p>The "allowEmpty" parameter controls whether an empty String is to be
	 * allowed in parsing, i.e. be interpreted as the {@code null} value when
	 * {@link #setAsText(String) text is being converted}. If {@code false},
	 * an {@link IllegalArgumentException} will be thrown at that time.
	 * @param allowEmpty if empty strings are to be allowed
	 */
	public CharacterEditor(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}


	@Override
	public void setAsText(@Nullable String text) throws IllegalArgumentException {
		if (this.allowEmpty && !StringUtils.hasLength(text)) {
			// Treat empty String as null value.
			setValue(null);
		}
		else if (text == null) {
			throw new IllegalArgumentException("null String cannot be converted to char type");
		}
		else if (isUnicodeCharacterSequence(text)) {
			setAsUnicode(text);
		}
		else if (text.length() == 1) {
			setValue(text.charAt(0));
		}
		else {
			throw new IllegalArgumentException("String [" + text + "] with length " +
					text.length() + " cannot be converted to char type: neither Unicode nor single character");
		}
	}

	@Override
	public String getAsText() {
		Object value = getValue();
		return (value != null ? value.toString() : "");
	}

	private static boolean isUnicodeCharacterSequence(String sequence) {
		return (sequence.startsWith(UNICODE_PREFIX) && sequence.length() == UNICODE_LENGTH);
	}

	private void setAsUnicode(String text) {
		int code = HexFormat.fromHexDigits(text, UNICODE_PREFIX.length(), text.length());
		setValue((char) code);
	}

}
