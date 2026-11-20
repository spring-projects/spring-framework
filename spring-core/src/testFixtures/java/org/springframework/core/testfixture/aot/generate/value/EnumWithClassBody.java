/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.testfixture.aot.generate.value;

/**
 * Test enum that include a class body.
 *
 * @author Phillip Webb
 */
public enum EnumWithClassBody {

	/**
	 * No class body.
	 */
	ONE,

	/**
	 * With class body.
	 */
	TWO {
		@Override
		public String toString() {
			return "2";
		}

	}

}
