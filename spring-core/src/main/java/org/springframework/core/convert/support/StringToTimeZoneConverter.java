/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.convert.support;

import java.util.TimeZone;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

/**
 * Convert a String to a {@link TimeZone}.
 *
 * @author Stephane Nicoll
 * @since 4.2
 */
class StringToTimeZoneConverter implements Converter<String, TimeZone> {

	@Override
	public TimeZone convert(String source) {
		return StringUtils.parseTimeZoneString(source);
	}

}
