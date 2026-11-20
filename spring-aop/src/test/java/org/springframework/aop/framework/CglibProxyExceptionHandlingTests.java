/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import org.springframework.cglib.proxy.Enhancer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mikaël Francoeur
 * @since 6.2
 * @see JdkProxyExceptionHandlingTests
 */
@DisplayName("CGLIB proxy exception handling")
class CglibProxyExceptionHandlingTests extends AbstractProxyExceptionHandlingTests {

	@BeforeEach
	void setup() {
		proxyFactory.setProxyTargetClass(true);
	}

	@Override
	protected void assertProxyType(Object proxy) {
		assertThat(Enhancer.isEnhanced(proxy.getClass())).isTrue();
	}

}
