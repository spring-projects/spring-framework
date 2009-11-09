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
package org.springframework.format.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a field should be formatted as a date time.
 * Supports formatting by style pattern or by format pattern string.
 * Can be applied to <code>java.util.Date</code>, <code>java.util.Calendar</code>, <code>java.long.Long</code>, or Joda Time fields.
 * <p>
 * For style-based formatting, set the <code>style</code> attribute to be the style pattern.  
 * The first character is the date style, and the second character is the time style.
 * Specify a character of 'S' for short style, 'M' for medium, 'L' for long, and 'F' for full.
 * A date or time may be omitted by specifying the style character '-'.
 * <p>
 * For pattern-based formatting, set the <code>pattern</code> attribute to be the DateTime pattern, such as <code>yyyy/mm/dd h:mm:ss a</code>.
 * If the pattern attribute is specified, it takes precedence over the style attribute.
 * <p>
 * If no annotation attributes are specified, the default format applied is style-based with a style code of 'SS' (short date, short time).
 * 
 * @author Keith Donald
 * @since 3.0
 * @see org.joda.time.format.DateTimeFormat
 */
@Target( { ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface DateTimeFormat {

	/**
	 * The style pattern to use to format the field.
	 * Defaults to 'SS' for short date time.
	 */
	String style() default "SS";

	/**
	 * A pattern String to apply to format the field.
	 * Set this attribute or the style attribute, not both.
	 */
	String pattern() default "";
		
}
