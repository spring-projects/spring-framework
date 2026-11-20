/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.expression.spel;

/**
 * This is intentionally a top-level public class.
 */
public class PublicSuperclass {

	public int process(int num) {
		return num + 1;
	}

	public int getNumber() {
		return 1;
	}

	public String getMessage() {
		return "goodbye";
	}

	public String greet(String name) {
		return "Super, " + name;
	}

	public String getIndex(int index) {
		return "value-" + index;
	}

	public String getIndex2(int index) {
		return "value-" + (2 * index);
	}

}
