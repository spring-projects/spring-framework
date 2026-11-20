/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.autoproxy;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Adrian Colyer
 * @author Juergen Hoeller
 */
class AutoProxyWithCodeStyleAspectsTests {

	@Test
	void noAutoProxyingOfAjcCompiledAspects() {
		new ClassPathXmlApplicationContext("org/springframework/aop/aspectj/autoproxy/ajcAutoproxyTests.xml");
	}

}
