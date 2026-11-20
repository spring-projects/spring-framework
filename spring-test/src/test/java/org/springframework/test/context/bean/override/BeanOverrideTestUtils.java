/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.context.bean.override;

import java.util.List;

/**
 * Test utilities for Bean Overrides.
 *
 * @author Sam Brannen
 * @since 6.2.2
 */
public abstract class BeanOverrideTestUtils {

	public static List<BeanOverrideHandler> findHandlers(Class<?> testClass) {
		return BeanOverrideHandler.forTestClass(testClass);
	}

	public static List<BeanOverrideHandler> findAllHandlers(Class<?> testClass) {
		return BeanOverrideHandler.findAllHandlers(testClass);
	}

}
