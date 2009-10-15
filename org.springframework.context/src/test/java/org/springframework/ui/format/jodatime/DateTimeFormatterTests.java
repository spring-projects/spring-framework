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

package org.springframework.ui.format.jodatime;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.junit.Test;

/**
 * @author Keith Donald
 */
public class DateTimeFormatterTests {

	private DateTimeFormatter formatter = new DateTimeFormatter("yyyy-MM-dd");
	
	@Test
	public void formatValue() {
		DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime("2009-06-01");
		assertEquals("2009-06-01", formatter.format(dateTime, Locale.US));
	}
	
	@Test
	public void parseValue() throws ParseException {
		DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime("2009-06-01");
		assertEquals(dateTime, formatter.parse("2009-06-01", Locale.US));
	}

}
