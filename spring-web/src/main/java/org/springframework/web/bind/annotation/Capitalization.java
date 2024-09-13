package org.springframework.web.bind.annotation;

public enum Capitalization {
	UPPER,
	LOWER,
	ORIGINAL;

	public Object capitalize(Object arg) {
		if (this != ORIGINAL && arg instanceof String txt) {
			if (this == UPPER) {
				return txt.toUpperCase();
			} else if (this == LOWER) {
				return txt.toLowerCase();
			}
		}
		return arg;
	}
}
