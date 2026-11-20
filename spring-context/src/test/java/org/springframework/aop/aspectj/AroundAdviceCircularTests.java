/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class AroundAdviceCircularTests extends AroundAdviceBindingTests {

	@Test
	void bothBeansAreProxies() {
		Object tb = ctx.getBean("testBean");
		assertThat(AopUtils.isAopProxy(tb)).isTrue();
		Object tb2 = ctx.getBean("testBean2");
		assertThat(AopUtils.isAopProxy(tb2)).isTrue();
	}

}
