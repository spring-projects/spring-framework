/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util.subpackage;

/**
 * Simple class with static methods; intended for use in unit tests.
 *
 * @author Sam Brannen
 * @since 5.2
 */
public class StaticMethods {

	public static String publicMethodValue = "public";

	private static String privateMethodValue = "private";


	public static void publicMethod(String value) {
		publicMethodValue = value;
	}

	public static String publicMethod() {
		return publicMethodValue;
	}

	@SuppressWarnings("unused")
	private static void privateMethod(String value) {
		privateMethodValue = value;
	}

	@SuppressWarnings("unused")
	private static String privateMethod() {
		return privateMethodValue;
	}

	public static void reset() {
		publicMethodValue = "public";
		privateMethodValue = "private";
	}

	public static String getPublicMethodValue() {
		return publicMethodValue;
	}

	public static String getPrivateMethodValue() {
		return privateMethodValue;
	}

}
