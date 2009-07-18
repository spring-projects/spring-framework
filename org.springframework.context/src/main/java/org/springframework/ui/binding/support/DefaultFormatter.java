/**
 * 
 */
package org.springframework.ui.binding.support;

import java.text.ParseException;
import java.util.Locale;

import org.springframework.ui.format.Formatter;

class DefaultFormatter implements Formatter<Object> {

	public static final Formatter<Object> INSTANCE = new DefaultFormatter();

	public String format(Object object, Locale locale) {
		if (object == null) {
			return "";
		} else {
			return object.toString();
		}
	}

	public Object parse(String formatted, Locale locale) throws ParseException {
		if (formatted == "") {
			return null;
		} else {
			return formatted;
		}
	}
}