/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.core.convert.support;

import java.util.regex.Pattern;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

/**
 * Converts from a String to a {@link java.util.regex.Pattern}.
 *
 * @author Valery Yatsynovich
 * @author Stephane Nicoll
 * @since 6.1
 */
final class StringToPatternConverter implements Converter<String, Pattern> {

	@Override
	@Nullable
	public Pattern convert(String source) {
		if (source.isEmpty()) {
			return null;
		}
		return Pattern.compile(source);
	}

}
