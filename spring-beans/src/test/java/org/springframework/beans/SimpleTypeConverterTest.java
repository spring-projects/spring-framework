package org.springframework.beans;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SimpleTypeConverterTest {

	@Test
	void stringToDate() {
		SimpleTypeConverter converter = new SimpleTypeConverter();
		converter.registerCustomEditor(Date.class, new SampleDatePropertyEditor());
		Date date = converter.convertIfNecessary("2022-10-13 00:00:00", Date.class);
		Assertions.assertNull(date);
	}


	class SampleDatePropertyEditor extends PropertyEditorSupport {

		@Override
		public void setAsText(String text) throws IllegalArgumentException {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try {
				setValue(sdf.parse(text));
			} catch (ParseException e) {
				throw new RuntimeException(e);
			}
		}
	}

}