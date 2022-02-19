/*
 * Copyright 2002-2013 the original author or authors.
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
import java.time.Duration;
import java.util.Objects;

import org.springframework.util.StringUtils;

/**
 * Editor for {@code java.time.Duration}, translating durations into
 * {@code Duration} objects. Exposes the {@code Duration} ISO as a text
 * representation.
 *
 * @author Davide Angelocola
 * @since 3.0
 * @see Duration
 */
public class DurationEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(StringUtils.parseDuration(text));
	}

	@Override
	public String getAsText() {
		Duration value = (Duration) getValue();
		return Objects.toString(value, "");
	}

}
