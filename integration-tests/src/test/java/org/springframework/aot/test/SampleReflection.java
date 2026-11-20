/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aot.test;

import java.lang.reflect.Method;

/**
 * @author Brian Clozel
 * @since 6.0
 */
public class SampleReflection {

	@SuppressWarnings("unused")
	public void sample() {
		String value = "Sample";
		Method[] methods = String.class.getMethods();
	}

	@SuppressWarnings("unused")
	public void multipleCalls() {
		String value = "Sample";
		Method[] methods = String.class.getMethods();
		methods = Integer.class.getMethods();
	}

}
