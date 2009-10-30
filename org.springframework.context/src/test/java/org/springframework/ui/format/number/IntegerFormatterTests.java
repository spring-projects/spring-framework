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

package org.springframework.ui.format.number;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;
import org.springframework.ui.format.number.IntegerFormatter;

/**
 * @author Keith Donald
 */
public class IntegerFormatterTests {

	private IntegerFormatter formatter = new IntegerFormatter();

	@Test
	public void formatValue() {
		assertEquals("23", formatter.print(23L, Locale.US));
	}

	@Test
	public void parseValue() throws ParseException {
		assertEquals((Long) 2356L, formatter.parse("2356", Locale.US));
	}

	@Test(expected = ParseException.class)
	public void parseBogusValue() throws ParseException {
		formatter.parse("bogus", Locale.US);
	}

	@Test(expected = ParseException.class)
	public void parsePercentValueNotLenientFailure() throws ParseException {
		formatter.parse("23.56", Locale.US);
	}

}
