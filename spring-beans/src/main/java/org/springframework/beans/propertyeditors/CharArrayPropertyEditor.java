/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

/**
 * Editor for char arrays. Strings will simply be converted to
 * their corresponding char representations.
 *
 * @author Juergen Hoeller
 * @since 1.2.8
 * @see String#toCharArray()
 */
public class CharArrayPropertyEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(@Nullable String text) {
		setValue(text != null ? text.toCharArray() : null);
	}

	@Override
	public String getAsText() {
		char[] value = (char[]) getValue();
		return (value != null ? new String(value) : "");
	}

}
