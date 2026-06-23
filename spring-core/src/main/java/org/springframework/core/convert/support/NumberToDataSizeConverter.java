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

package org.springframework.core.convert.support;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.unit.DataSize;

/**
 * Converts from a {@link Number} to a {@link DataSize}.
 *
 * @author YeongJae Min
 * @author Yanming Zhou
 * @since 7.1
 * @see DataSize#ofBytes(long)
 * @see StringToDataSizeConverter
 */
final class NumberToDataSizeConverter implements Converter<Number, DataSize> {

	@Override
	public DataSize convert(Number source) {
		long bytes = source.longValue();
		// Ensure Number is a whole number.
		if (source.doubleValue() - bytes != 0) {
			throw new IllegalArgumentException("'" + source + "' is not a valid data size");
		}
		return DataSize.ofBytes(bytes);
	}

}
