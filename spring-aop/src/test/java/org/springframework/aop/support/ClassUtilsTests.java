/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.support;

import org.junit.jupiter.api.Test;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AOP-specific tests for {@link ClassUtils}.
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Rick Evans
 */
class ClassUtilsTests {

	@Test
	void getShortNameForCglibClass() {
		TestBean tb = new TestBean();
		ProxyFactory pf = new ProxyFactory();
		pf.setTarget(tb);
		pf.setProxyTargetClass(true);
		TestBean proxy = (TestBean) pf.getProxy();
		String className = ClassUtils.getShortName(proxy.getClass());
		assertThat(className).as("Class name did not match").isEqualTo("TestBean");
	}

}
