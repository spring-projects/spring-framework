/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.scope;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultScopedObject}.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
class DefaultScopedObjectTests {

	private static final String GOOD_BEAN_NAME = "foo";


	@Test
	void ctorWithNullBeanFactory() {
		assertThatIllegalArgumentException().isThrownBy(() ->
			new DefaultScopedObject(null, GOOD_BEAN_NAME));
	}

	@Test
	void ctorWithNullTargetBeanName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				testBadTargetBeanName(null));
	}

	@Test
	void ctorWithEmptyTargetBeanName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				testBadTargetBeanName(""));
	}

	@Test
	void ctorWithJustWhitespacedTargetBeanName() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				testBadTargetBeanName("   "));
	}

	private static void testBadTargetBeanName(final String badTargetBeanName) {
		ConfigurableBeanFactory factory = mock();
		new DefaultScopedObject(factory, badTargetBeanName);
	}

}
