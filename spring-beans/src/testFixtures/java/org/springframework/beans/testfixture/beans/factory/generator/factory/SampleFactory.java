/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator.factory;

public abstract class SampleFactory {

	public static String create(String testBean) {
		return testBean;
	}

	public static String create(char character) {
		return String.valueOf(character);
	}

	public static String create(Number number, String test) {
		return number + test;
	}

	public static String create(Class<?> type) {
		return type.getName();
	}

	public static Integer integerBean() {
		return 42;
	}

}
