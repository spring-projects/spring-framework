/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.DisplayName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mikaël Francoeur
 * @since 6.2
 * @see CglibProxyExceptionHandlingTests
 */
@DisplayName("JDK proxy exception handling")
class JdkProxyExceptionHandlingTests extends AbstractProxyExceptionHandlingTests {

	@Override
	protected void assertProxyType(Object proxy) {
		assertThat(Proxy.isProxyClass(proxy.getClass())).isTrue();
	}

}
