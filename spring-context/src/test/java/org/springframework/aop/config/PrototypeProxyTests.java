/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.config;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class PrototypeProxyTests {

	@Test
	@SuppressWarnings("resource")
	void injectionBeforeWrappingCheckDoesNotKickInForPrototypeProxy() {
		new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
	}

}
