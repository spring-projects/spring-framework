/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.aspectj.generic;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for AspectJ pointcut expression matching when working with bridge methods.
 *
 * <p>This class focuses on class proxying.
 *
 * <p>See {@link GenericBridgeMethodMatchingTests} for more details.
 *
 * @author Ramnivas Laddad
 * @author Chris Beams
 */
class GenericBridgeMethodMatchingClassProxyTests extends GenericBridgeMethodMatchingTests {

	@Test
	void genericDerivedInterfaceMethodThroughClass() {
		((DerivedStringParameterizedClass) testBean).genericDerivedInterfaceMethod("");
		assertThat(counterAspect.count).isEqualTo(1);
	}

	@Test
	void genericBaseInterfaceMethodThroughClass() {
		((DerivedStringParameterizedClass) testBean).genericBaseInterfaceMethod("");
		assertThat(counterAspect.count).isEqualTo(1);
	}

}
