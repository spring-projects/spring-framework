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

package org.springframework.format.datetime;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

import static org.junit.Assert.*;
import org.junit.Test;
import org.springframework.format.datetime.DateFormatter;

/**
 * @author Keith Donald
 */
public class DateFormatterTests {

	private DateFormatter formatter = new DateFormatter("yyyy-MM-dd");
	
	@Test
	public void formatValue() {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.clear();
		cal.set(Calendar.YEAR, 2009);
		cal.set(Calendar.MONTH, Calendar.JUNE);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		assertEquals("2009-06-01", formatter.print(cal.getTime(), Locale.US));
	}
	
	@Test
	public void parseValue() throws ParseException {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.clear();
		cal.set(Calendar.YEAR, 2009);
		cal.set(Calendar.MONTH, Calendar.JUNE);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		assertEquals(cal.getTime(), formatter.parse("2009-06-01", Locale.US));
	}

}
