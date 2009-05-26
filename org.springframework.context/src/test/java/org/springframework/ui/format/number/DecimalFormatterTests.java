package org.springframework.ui.format.number;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;

public class DecimalFormatterTests {

	private DecimalFormatter formatter = new DecimalFormatter();

	@Test
	public void formatValue() {
		assertEquals("23.56", formatter.format(new BigDecimal("23.56"), Locale.US));
	}

	@Test
	public void parseValue() throws ParseException {
		assertEquals(new BigDecimal("23.56"), formatter.parse("23.56", Locale.US));
	}

	@Test
	public void parseEmptyValue() throws ParseException {
		assertEquals(null, formatter.parse("", Locale.US));
	}

	@Test(expected = ParseException.class)
	public void parseBogusValue() throws ParseException {
		formatter.parse("bogus", Locale.US);
	}

	@Test(expected = ParseException.class)
	public void parsePercentValueNotLenientFailure() throws ParseException {
		formatter.parse("23.56bogus", Locale.US);
	}

}
