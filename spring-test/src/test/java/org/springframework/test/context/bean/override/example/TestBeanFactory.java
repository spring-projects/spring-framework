/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override.example;

/**
 * Interface that exposes a single method that is used by bean overriding
 * tests.
 *
 * @author Sam Brannen
 * @since 6.2
 */
public interface TestBeanFactory {

	static String createTestMessage() {
		return "test";
	}

}
