/*
 * Copyright 2002-2009 the original author or authors.
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
package org.springframework.format.datetime.joda;

import org.joda.time.format.DateTimeFormatter;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.Style;

/**
 * Formats fields annotated with the {@link DateTimeFormat} annotation.
 * @author Keith Donald
 * @since 3.0
 * @see DateTimeFormat
 */
public final class DateTimeFormatAnnotationFormatterFactory extends AbstractDateTimeAnnotationFormatterFactory<DateTimeFormat> {

	protected DateTimeFormatter configureDateTimeFormatterFrom(DateTimeFormat annotation) {
		String pattern = annotation.pattern();
		if (!pattern.isEmpty()) {
			return forPattern(pattern);
		} else {
			Style dateStyle = annotation.dateStyle();
			Style timeStyle = annotation.timeStyle();
			if (Style.NONE == dateStyle && Style.NONE == timeStyle) {
				return forDateTimeStyle(Style.SHORT, Style.SHORT);
			} else {
				return forDateTimeStyle(dateStyle, timeStyle);
			}
		}
	}

	private DateTimeFormatter forDateTimeStyle(Style dateStyle, Style timeStyle) {
		return org.joda.time.format.DateTimeFormat.forStyle(dateStyle.toString() + timeStyle.toString());
	}

	private DateTimeFormatter forPattern(String pattern) {
		return org.joda.time.format.DateTimeFormat.forPattern(pattern);
	}

}