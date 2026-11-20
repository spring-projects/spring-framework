/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import org.springframework.stereotype.Component;

@Component class Dependency {

	private int value = 0;

	public void method() {
		value++;
	}

	public int getValue() {
		return value;
	}
}
