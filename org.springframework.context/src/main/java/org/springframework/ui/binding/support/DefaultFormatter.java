/**
 * 
 */
package org.springframework.ui.binding.support;

import java.lang.reflect.Array;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;

import org.springframework.ui.format.Formatter;

@SuppressWarnings("unchecked")
class DefaultFormatter implements Formatter {

	public static final Formatter INSTANCE = new DefaultFormatter();

	public static final Formatter COLLECTION_FORMATTER = new Formatter() {
		public String format(Object object, Locale locale) {
			if (object == null) {
				return "";
			} else {
				StringBuffer buffer = new StringBuffer();
				if (object.getClass().isArray()) {
					int length = Array.getLength(object);
					for (int i = 0; i < length; i++) {
						buffer.append(INSTANCE.format(Array.get(object, i), locale));
						if (i < length - 1) {
							buffer.append(",");
						}
					}
				} else if (Collection.class.isAssignableFrom(object.getClass())) {
					Collection c = (Collection) object;
					for (Iterator it = c.iterator(); it.hasNext();) {
						buffer.append(INSTANCE.format(it.next(), locale));
						if (it.hasNext()) {
							buffer.append(",");
						}
					}
				}
				return buffer.toString();
			}
		}

		public Object parse(String formatted, Locale locale) throws ParseException {
			throw new UnsupportedOperationException("Not yet implemented");
		}
		
	};
	
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