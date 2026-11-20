/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.validation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class ValidatorTests {

	@Test
	void supportsForInstanceOf() {
		Validator validator = Validator.forInstanceOf(TestBean.class, (testBean, errors) -> {});
		assertThat(validator.supports(TestBean.class)).isTrue();
		assertThat(validator.supports(TestBeanSubclass.class)).isTrue();
	}

	@Test
	void supportsForType() {
		Validator validator = Validator.forType(TestBean.class, (testBean, errors) -> {});
		assertThat(validator.supports(TestBean.class)).isTrue();
		assertThat(validator.supports(TestBeanSubclass.class)).isFalse();
	}


	private static class TestBeanSubclass extends TestBean {
	}

}
