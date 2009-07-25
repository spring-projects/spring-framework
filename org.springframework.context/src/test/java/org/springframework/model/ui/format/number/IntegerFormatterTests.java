package org.springframework.model.ui.format.number;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Locale;

import org.junit.Test;
import org.springframework.model.ui.format.number.IntegerFormatter;

public class IntegerFormatterTests {

	private IntegerFormatter formatter = new IntegerFormatter();

	@Test
	public void formatValue() {
		assertEquals("23", formatter.format(23L, Locale.US));
	}

	@Test
	public void parseValue() throws ParseException {
		assertEquals((Long) 2356L, formatter.parse("2356", Locale.US));
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
		formatter.parse("23.56", Locale.US);
	}

}
