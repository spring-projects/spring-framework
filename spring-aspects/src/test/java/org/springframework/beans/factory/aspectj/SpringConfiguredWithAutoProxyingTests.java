/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Ramnivas Laddad
 * @author Juergen Hoeller
 */
class SpringConfiguredWithAutoProxyingTests {

	@Test
	void springConfiguredAndAutoProxyUsedTogether() {
		new ClassPathXmlApplicationContext("org/springframework/beans/factory/aspectj/springConfigured.xml");
	}

}
