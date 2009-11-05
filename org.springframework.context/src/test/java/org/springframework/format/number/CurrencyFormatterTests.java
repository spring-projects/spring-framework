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

package org.springframework.format.number;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;
import org.springframework.format.number.CurrencyFormatter;

/**
 * @author Keith Donald
 */
public class CurrencyFormatterTests {

	private CurrencyFormatter formatter = new CurrencyFormatter();
	
	@Test
	public void formatValue() {
		assertEquals("$23.00", formatter.print(new BigDecimal("23"), Locale.US));
	}

	@Test
	public void parseValue() throws ParseException {
		assertEquals(new BigDecimal("23.56"), formatter.parse("$23.56", Locale.US));
	}

	@Test(expected = ParseException.class)
	public void parseBogusValue() throws ParseException {
		formatter.parse("bogus", Locale.US);
	}

	@Test
	public void parseValueDefaultRoundDown() throws ParseException {
		this.formatter.setRoundingMode(RoundingMode.DOWN);
		assertEquals(new BigDecimal("23.56"), formatter.parse("$23.567", Locale.US));
	}

	@Test
	public void parseWholeValue() throws ParseException {
		assertEquals(new BigDecimal("23.00"), formatter.parse("$23", Locale.US));
	}

	@Test(expected=ParseException.class)
	public void parseValueNotLenientFailure() throws ParseException {
		formatter.parse("$23.56bogus", Locale.US);
	}

}
