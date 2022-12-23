package org.springframework.beans;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import org.junit.jupiter.api.Test;

class SimpleTypeConverterTest {
	@Test
	void stringToDate(){
		SimpleTypeConverter converter = new SimpleTypeConverter();
		Date date = converter.convertIfNecessary("2022-10-13 00:00:00", Date.class);
		System.out.println();
	}

}