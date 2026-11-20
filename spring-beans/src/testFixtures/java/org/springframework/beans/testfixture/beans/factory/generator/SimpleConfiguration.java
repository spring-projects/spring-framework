/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans.factory.generator;

import java.io.IOException;

public class SimpleConfiguration {

	public String stringBean() {
		return "Hello";
	}

	@SuppressWarnings("unused")
	private static String privateStaticStringBean() {
		return "Hello";
	}

	static String packageStaticStringBean() {
		return "Hello";
	}

	public static Integer integerBean() {
		return 42;
	}

	public Integer throwingIntegerBean() throws IOException {
		return 42;
	}

}
