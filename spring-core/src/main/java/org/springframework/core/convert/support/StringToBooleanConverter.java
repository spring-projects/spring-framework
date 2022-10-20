/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.Nullable;

/**
 * Converts a String to a Boolean.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.0
 */
final class StringToBooleanConverter implements Converter<String, Boolean> {

	private static final Set<String> trueValues = Set.of("true", "on", "yes", "1");

	private static final Set<String> falseValues = Set.of("false", "off", "no", "0");


	@Override
	@Nullable
	public Boolean convert(String source) {
		String value = source.trim();
		if (value.isEmpty()) {
			return null;
		}
		value = value.toLowerCase();
		if (trueValues.contains(value)) {
			return Boolean.TRUE;
		}
		else if (falseValues.contains(value)) {
			return Boolean.FALSE;
		}
		else {
			throw new IllegalArgumentException("Invalid boolean value '" + source + "'");
		}
	}

}
