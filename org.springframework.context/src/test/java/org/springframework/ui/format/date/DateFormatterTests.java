package org.springframework.ui.format.date;

import static org.junit.Assert.assertEquals;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Locale;

import org.junit.Test;

public class DateFormatterTests {

	private DateFormatter formatter = new DateFormatter();
	
	@Test
	public void formatValue() {
		Calendar cal = Calendar.getInstance(Locale.US);
		cal.clear();
		cal.set(Calendar.YEAR, 2009);
		cal.set(Calendar.MONTH, Calendar.JUNE);
		cal.set(Calendar.DAY_OF_MONTH, 1);
		assertEquals("2009-06-01", formatter.format(cal.getTime(), Locale.US));
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
