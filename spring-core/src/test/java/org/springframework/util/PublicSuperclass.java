/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

/**
 * This is intentionally a top-level public class.
 */
public class PublicSuperclass {

	/**
	 * This method intentionally has the exact same signature as
	 * {@link org.springframework.util.ClassUtilsTests.PublicSubclass#getCacheKey()} and
	 * {@link org.springframework.util.ClassUtilsTests.PrivateSubclass#getCacheKey()}.
	 */
	public static String getCacheKey() {
		return "parent";
	}

	public String getMessage() {
		return "goodbye";
	}

	public int getNumber() {
		return 1;
	}

	public String greet(String name) {
		return "Super, " + name;
	}

	public int process(int num) {
		return num + 1;
	}

}
