package org.springframework.model.ui.format.number;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;
import org.springframework.model.ui.format.number.PercentFormatter;

public class PercentFormatterTests {

	private PercentFormatter formatter = new PercentFormatter();

	@Test
	public void formatValue() {
		assertEquals("23%", formatter.format(new BigDecimal(".23"), Locale.US));
	}

	@Test
	public void parseValue() throws ParseException {
		assertEquals(new BigDecimal(".2356"), formatter.parse("23.56%",
				Locale.US));
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
		formatter.parse("23.56%bogus", Locale.US);
	}

}
