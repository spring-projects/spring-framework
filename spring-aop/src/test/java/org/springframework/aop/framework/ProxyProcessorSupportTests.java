/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import org.springframework.cglib.proxy.Factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ProxyProcessorSupport}.
 *
 * @author Sam Brannen
 * @since 7.0.10
 */
class ProxyProcessorSupportTests {

	private final ProxyProcessorSupport proxyProcessorSupport = new ProxyProcessorSupport();


	@Test
	void isInternalLanguageInterfaceForCglibProxyFactory() {
		assertThat(proxyProcessorSupport.isInternalLanguageInterface(Factory.class)).isTrue();
	}

	@Test
	void isInternalLanguageInterfaceForCurrentMockitoMockAccess() {
		Class<?> mockAccessInterface = Arrays.stream(mock(Runnable.class).getClass().getInterfaces())
				.filter(ifc -> ifc.getSimpleName().equals("MockAccess"))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Mockito mock does not implement a MockAccess interface"));

		assertThat(mockAccessInterface.getName()).endsWith(".bytebuddy.access.MockAccess");
		assertThat(proxyProcessorSupport.isInternalLanguageInterface(mockAccessInterface)).isTrue();
	}

	@Test
	void isInternalLanguageInterfaceForLegacyMockitoMockAccess() {
		// Mockito relocated MockAccess in 5.16.1 from a "bytebuddy" package to a
		// "bytebuddy.access" subpackage. This interface mimics the pre-5.16.1
		// location so that we can verify that both locations are supported.
		assertThat(proxyProcessorSupport.isInternalLanguageInterface(
				org.springframework.aop.framework.bytebuddy.MockAccess.class)).isTrue();
	}

	@Test
	void isNotInternalLanguageInterface() {
		assertThat(proxyProcessorSupport.isInternalLanguageInterface(Runnable.class)).isFalse();
	}

}
